package controllers.v1

import javax.inject._

import cats.data.ValidatedNel
import com.outworkers.util.catsparsers.{parse => cparse, _}
import com.outworkers.util.play._
import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import controllers.v1.BusinessIndexObj._
import nl.grons.metrics.scala.DefaultInstrumented
import org.elasticsearch.client.transport.NoNodeAvailableException
import play.api.libs.json._
import play.api.mvc._
import services.HBaseCache
import uk.gov.ons.bi.models.{BIndexConsts, BusinessIndexRec}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

object BusinessIndexObj {
  implicit val businessHitFormat: OFormat[BusinessIndexRec] = Json.format[BusinessIndexRec]
}

/**
  * Contains action for the /v1/search route.
  *
  * @param elastic
  * @param context
  * @param config
  */
@Singleton
class SearchController @Inject()(elastic: ElasticClient, val config: Config)(
  implicit context: ExecutionContext
) extends Controller with ElasticDsl with DefaultInstrumented with StrictLogging with HBaseCache {

  implicit object LongParser extends CatsParser[Long] {
    override def parse(str: String): ValidatedNel[String, Long] = {
      Try(java.lang.Long.parseLong(str)).asValidation
    }
  }

  override protected def tableName = "es_requests"

  // metrics
  private[this] val requestMeter = metrics.meter("search-requests", "requests")
  private[this] val totalHitsHistogram = metrics.histogram("totalHits", "es-searches")

  private[this] val index = config.getString("elasticsearch.bi.name").concat("/business")

  // mapper from ElasticSearch result to Business case class
  implicit object BusinessHitAs extends HitAs[BusinessIndexRec] {
    override def as(hit: RichSearchHit): BusinessIndexRec = BusinessIndexRec.fromMap(hit.id.toLong, hit.sourceAsMap)
  }

  protected[this] def businessSearch(term: String, offset: Int, limit: Int, suggest: Boolean = false
                                    ): Future[(RichSearchResponse, List[BusinessIndexRec])] = {
    val definition = if (suggest) {
      matchQuery(BIndexConsts.BiName, query)
    } else {
      QueryStringQueryDefinition(term)
    }

    val r = elastic.execute {
      search.in(index)
        .query(definition)
        .start(offset)
        .limit(limit)
    }.map { resp =>
      if (resp.shardFailures.nonEmpty)
        sys.error(s"${resp.shardFailures.length} failed shards out of ${resp.totalShards}, the returned result would be partial and not reliable")

      resp.as[BusinessIndexRec].toList match {
        case list@_ :: _ =>
          totalHitsHistogram += resp.totalHits
          resp -> list
        case Nil => resp -> List.empty[BusinessIndexRec]
      }
    }
    r
  }

  def response(resp: SearchData, businesses: List[BusinessIndexRec]): Result = {
    businesses match {
      case _ :: _ => responseWithHTTPHeaders(resp, Ok(Json.toJson(businesses.map(_.secured))))
      case _ => responseWithHTTPHeaders(resp, Ok("{}").as(JSON))
    }
  }

  def response(tp: (SearchData, List[BusinessIndexRec])): Result = response(tp._1, tp._2)

  case class SearchData(totalHits: Long, maxScore: Float)

  def responseWithHTTPHeaders(resp: SearchData, searchResult: Result): Result = {
    searchResult.withHeaders(
      "X-Total-Count" -> resp.totalHits.toString,
      "X-Max-Score" -> resp.maxScore.toString)
  }

  def searchTerm(term: String, suggest: Boolean = false): Action[AnyContent] = searchBusiness(Some(term), suggest)

  protected[this] def resultAsBusiness(businessId: Long, resp: RichGetResponse): Option[BusinessIndexRec] = {
    val source = Option(resp.source).map(_.asScala.toMap[String, AnyRef]).getOrElse(Map.empty[String, AnyRef])

    Try(BusinessIndexRec.fromMap(businessId, source)).toOption
  }

  def findById(businessId: Long): Future[Option[BusinessIndexRec]] = {
    logger.debug(s"Searching for business with ID $businessId")
    elastic.execute {
      get id businessId from index
    } map (resultAsBusiness(businessId, _))
  }

  def searchBusinessById(id: String): Action[AnyContent] = Action.async {
    cparse[Long](id) fold(_.response.future, value =>
      findById(value) map {
        case Some(res) =>
          Ok(Json.toJson(res.secured))
        case None =>
          logger.debug(s"Could not find a record with the ID $id")
          NoContent
      }
    )
  }

  private[this] val isCaching = config.getBoolean("hbase.caching.enabled")

  private[this] def getOrElseWrap(request: String)(f: => Future[(RichSearchResponse, List[BusinessIndexRec])]):
  Future[(SearchData, List[BusinessIndexRec])] = {
    if (isCaching) {
      getFromCache(request) match {
        case Some(s) =>
          val cachedBus = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).getOrElse(sys.error("Unable to extract json"))
          Future.successful((SearchData(cachedBus.size, 100), cachedBus))
        case None => f.map { case (r, businesses) =>
          updateCache(request, Json.toJson(businesses).toString())
          (SearchData(r.totalHits, r.maxScore), businesses)
        }
      }
    } else {
      f.map { case (r, businesses) => (SearchData(r.totalHits, r.maxScore), businesses) }
    }
  }

  def searchBusiness(term: Option[String], suggest: Boolean = false): Action[AnyContent] = {
    Action.async { implicit request =>
      // getOrElseWrap(term)
      requestMeter.mark()

      val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

      val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
      val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

      searchTerm match {
        case Some(query) if query.length > 0 =>
          // if suggest, match on the BusinessName only, else assume it's an Elasticsearch query
          getOrElseWrap(query) {
            businessSearch(query, offset, limit, suggest)
          } map response recover {
            case e: NoNodeAvailableException => ServiceUnavailable(
              Json.obj(
                "status" -> 503,
                "code" -> "es_down",
                "message_en" -> e.getMessage
              )
            )
            case NonFatal(e) =>
              logger.error(s"Internal error ${e.getMessage}", e)
              InternalServerError(
              Json.obj(
                "status" -> 500,
                "code" -> "internal_error",
                "message_en" -> e.getMessage
              )
            )
          }
        case _ =>
          BadRequest(
            Json.obj(
              "status" -> 400,
              "code" -> "missing_query",
              "message_en" -> "No query specified."
            )
          ).future
      }
    }
  }

}
