package controllers.v1

import javax.inject._

import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.StrictLogging
import nl.grons.metrics.scala.DefaultInstrumented
import org.elasticsearch.client.transport.NoNodeAvailableException
import play.api.Environment
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

case class Business(
  id: Long,
  businessName: String,
  uprn: Long,
  industryCode: Long,
  legalStatus: String,
  tradingStatus: String,
  turnover: String,
  employmentBands: String
)

object Business {
  implicit val businessHitFormat: OFormat[Business] = Json.format[Business]
}

/**
  * Contains action for the /v1/search route.
  *
  * @param environment
  * @param elastic
  * @param context
  */
@Singleton
class SearchController @Inject()(
  environment: Environment,
  elastic: ElasticClient
)(
  implicit context: ExecutionContext
) extends Controller with ElasticDsl with DefaultInstrumented with StrictLogging {

  // metrics
  private[this] val requestMeter = metrics.meter("search-requests", "requests")
  private[this] val totalHitsHistogram = metrics.histogram("totalHits", "es-searches")

  private[this] val index = s"bi-${environment.mode.toString.toLowerCase}" / "business"

  // mapper from Elasticsearch result to Business case class
  implicit object BusinessHitAs extends HitAs[Business] {
    override def as(hit: RichSearchHit): Business = {
      Business(
        hit.id.toLong,
        hit.sourceAsMap("BusinessName").toString,
        hit.sourceAsMap("UPRN").toString.toLong,
        hit.sourceAsMap("IndustryCode").toString.toLong,
        hit.sourceAsMap("LegalStatus").toString,
        hit.sourceAsMap("TradingStatus").toString,
        hit.sourceAsMap("Turnover").toString,
        hit.sourceAsMap("EmploymentBands").toString
      )
    }
  }

  protected[this] def businessSearch(
    term: String,
    offset: Int,
    limit: Int,
    suggest: Boolean = false
  ): Future[(RichSearchResponse, List[Business])] = {
    val definition = if (suggest) {
      matchQuery("BusinessName", query)
    } else {
      QueryStringQueryDefinition(term)
    }

    elastic.execute {
      search.in(index)
        .query(definition)
        .start(offset)
        .limit(limit)
    }.map { resp => resp.as[Business].toList match {
        case list@_ :: _ => {
          totalHitsHistogram += resp.totalHits
          resp -> list
        }
        case Nil => resp -> List.empty[Business]
      }
    }
  }

  def response(resp: RichSearchResponse, businesses: List[Business]): Result = {
    businesses match {
      case _ :: _ => {
        Ok(Json.toJson(businesses))
          .withHeaders(
            "X-Total-Count" -> resp.totalHits.toString,
            "X-Max-Score" -> resp.maxScore.toString
          )
      }
      case _ => Ok("{}").as(JSON)
    }
  }

  def response(tp: (RichSearchResponse, List[Business])): Result = response(tp._1, tp._2)

  def searchTerm(term: String, suggest: Boolean = false): Action[AnyContent] = searchBusiness(Some(term), suggest)

  protected[this] def resultAsBusiness(businessId: Long, resp: RichGetResponse): Option[Business] = {
    Console.println(resp.sourceAsString)
    val source = Option(resp.source).map(_.asScala.toMap[String, AnyRef]).getOrElse(Map.empty[String, AnyRef])

    for {
      name <- source.get("BusinessName")
      uprn <- source.get("UPRN")
      code <- source.get("IndustryCode")
      legalStatus <- source.get("LegalStatus")
      tradingStatus <- source.get("TradingStatus")
      turnover <- source.get("Turnover")
      bands <- source.get("EmploymentBands")
    } yield {
      Business(
        id = businessId,
        businessName = name.toString,
        uprn = uprn.toString.toLong,
        industryCode = code.toString.toLong,
        legalStatus = legalStatus.toString,
        tradingStatus = tradingStatus.toString,
        turnover = turnover.toString,
        employmentBands = bands.toString
      )
    }
  }

  def findById(businessId: Long): Future[Option[Business]] = {
    elastic.execute { get id id from index } map(resultAsBusiness(businessId, _))
  }

  def searchBusinessById(id: String): Action[AnyContent] = Action.async {
    Try(id.toLong) match {
      case Success(value) => findById(value) map {
        case Some(res) => Ok(Json.toJson(res))
        case None => NoContent
      }
      case Failure(err) => Future.successful(InternalServerError(err.getMessage))
    }
  }

  def searchBusiness(term: Option[String], suggest: Boolean = false): Action[AnyContent] = {
    Action.async { implicit request =>
      requestMeter.mark()

      val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

      val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
      val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

      searchTerm match {
        case Some(query) if query.length > 0 =>
          // if suggest, match on the BusinessName only, else assume it's an Elasticsearch query
          businessSearch(query, offset, limit, suggest) map response recover {
            case e: NoNodeAvailableException => ServiceUnavailable(
              Json.obj(
                "status" -> 503,
                "code" -> "es_down",
                "message_en" -> e.getMessage
              )
            )

            case NonFatal(e) => InternalServerError(
              Json.obj(
                "status" -> 500,
                "code" -> "internal_error",
                "message_en" -> e.getMessage
              )
            )
          }
        case _ =>
          Future.successful(
            BadRequest(Json.obj("status" -> 400, "code" -> "missing_query", "message_en" -> "No query specified."))
          )
      }
    }
  }

}
