package controllers.v1

import javax.inject._

import cats.data.ValidatedNel
import com.outworkers.util.catsparsers.{parse => cparse, _}
import com.outworkers.util.play._
import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import controllers.v1.BusinessIndexObj._
import io.swagger.annotations._
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.libs.json._
import play.api.mvc._
import services.HBaseCache
import uk.gov.ons.bi.models.{BIndexConsts, BusinessIndexRec}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
@Api("Search")
@Singleton
class SearchController @Inject()(elastic: ElasticClient, val config: Config)(
  implicit context: ExecutionContext
) extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with HBaseCache {

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

  protected[this] def businessSearchInternal(term: String, offset: Int, limit: Int,
                                             suggest: Boolean = false,
                                             defaultOperator: String): Future[RichSearchResponse] = {
    val definition = if (suggest) {
      matchQuery(BIndexConsts.cBiName, query)
    } else {
      QueryStringQueryDefinition(term).defaultOperator(defaultOperator)
    }
    elastic.execute {
      search.in(index)
        .query(definition)
        .start(offset)
        .limit(limit)
    }.map { resp =>
      if (resp.shardFailures.nonEmpty)
        sys.error(s"${resp.shardFailures.length} failed shards out of ${resp.totalShards}, the returned result would be partial and not reliable")
      resp
    }
  }

  // search with limit=0 still returns count of elements
  private[this] def businessSearch(term: String, offset: Int, limit: Int, suggest: Boolean = false,
                                   defaultOperator: String): Future[(RichSearchResponse, List[BusinessIndexRec])] = {
    businessSearchInternal(term, offset, limit, suggest, defaultOperator).map { resp =>
      logger.trace(s"Business search term $term, offset: $offset, limit: $limit, operator: $defaultOperator - response: $resp")
      resp.as[BusinessIndexRec].toList match {
        case list@_ :: _ =>
          totalHitsHistogram += resp.totalHits
          resp -> list
        case Nil => resp -> List.empty[BusinessIndexRec]
      }
    }
  }

  private[this] val isCaching = config.getBoolean("hbase.caching.enabled")

  // hbase caching
  private[this] def getOrElseWrap(request: String)(f: => Future[(RichSearchResponse, List[BusinessIndexRec])]):
  Future[(SearchData, List[BusinessIndexRec])] = {
    if (isCaching) {
      getFromCache(request) match {
        case Some(s) =>
          val cachedBus = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).getOrElse(sys.error("Unable to extract json"))
          Future.successful((SearchData(cachedBus.size.toLong, 100f), cachedBus))
        case None => f.map { case (r, businesses) =>
          updateCache(request, Json.toJson(businesses).toString())
          (SearchData(r.totalHits, r.maxScore), businesses)
        }
      }
    } else {
      f.map { case (r, businesses) => (SearchData(r.totalHits, r.maxScore), businesses) }
    }
  }

  // public API
  @ApiOperation(value = "Search businesses by query",
    notes = "Returns list of available businesses",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal server error"),
    new ApiResponse(code = 503, message = "Elastic search is not available")))
  def searchTerm(@ApiParam(value = "Query to elastic search") term: String, suggest: Boolean = false): Action[AnyContent] = searchBusiness(Some(term), suggest)

  private[this] def findById(businessId: Long): Future[Option[BusinessIndexRec]] = {
    logger.debug(s"Searching for business with ID $businessId")
    elastic.execute {
      get id businessId from index
    } map (resultAsBusiness(businessId, _))
  }

  // public API
  @ApiOperation(
    value = "Search businesses by UBRN",
    notes = "Returns exact business index record for particular UBRN Request",
    httpMethod = "GET")
  def searchBusinessById(@ApiParam(value = "UBRN to search") id: String): Action[AnyContent] = Action.async {
    cparse[Long](id) fold(_.response.future, value =>
      findById(value) map {
        case Some(res) =>
          Ok(Json.toJson(res))
        case None =>
          logger.debug(s"Could not find a record with the ID $id")
          NoContent
      }
    )
  }

  // public api
  @ApiOperation(value = "Search businesses by query",
    notes = "Returns list of available businesses. Additional parameters: offset, limit, default_operator",
    httpMethod = "GET")
  def searchBusiness(@ApiParam(value = "Query to elastic search") term: Option[String], suggest: Boolean = false): Action[AnyContent] = {
    Action.async { implicit request =>
      // getOrElseWrap(term)
      requestMeter.mark()

      val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

      val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
      val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)
      val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")
      val failOnQueryError = Try(request.getQueryString("fail_on_bad_query").getOrElse("true").toBoolean).getOrElse(true)

      searchTerm match {
        case Some(query) if query.length > 0 =>
          // if suggest, match on the BusinessName only, else assume it's an ElasticSearch query
          getOrElseWrap(query) {
            businessSearch(query, offset, limit, suggest, defaultOperator)
          } map response recover responseRecover(query, failOnQueryError)
        case _ =>
          BadRequest(errAsJson(400, "missing_query", "No query specified.")).future
      }
    }
  }
}
