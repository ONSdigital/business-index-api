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
import scala.util.Try
import scala.util.control.NonFatal

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
  implicit val businessHitFormat = Json.format[Business]
}

/**
  * Contains action for the /v1/search route.
  *
  * @param environment
  * @param elasticsearchClient
  * @param context
  */
@Singleton
class SearchController @Inject()(
  environment: Environment,
  elasticsearchClient: ElasticClient
)(
  implicit context: ExecutionContext
) extends Controller with ElasticDsl with DefaultInstrumented with StrictLogging {

  // metrics
  private[this] val requestMeter = metrics.meter("search-requests", "requests")
  private[this] val totalHitsHistogram = metrics.histogram("totalHits", "es-searches")

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

    elasticsearchClient.execute {
      search.in(s"bi-${environment.mode.toString.toLowerCase}" / "business")
        .query(definition)
        .start(offset)
        .limit(limit)
    }.map { resp => resp.as[Business].toList match {
        case list@head :: tail => {
          totalHitsHistogram += resp.totalHits
          resp -> list
        }
        case Nil => resp -> List.empty[Business]
      }
    }
  }

  def response(resp: RichSearchResponse, businesses: List[Business]): Result = {
    businesses match {
      case head :: tail => {
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

  def searchTerm(term: String, suggest: Boolean = false) = searchBusiness(Some(term), suggest)

  def searchBusiness(term: Option[String], suggest: Boolean = false) = Action.async { implicit request =>
    requestMeter.mark()

    val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

    val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    searchTerm match {
      case Some(query) if query.length > 0 =>
        // if suggest, match on the BusinessName only, else assume it's an Elasticsearch query
        businessSearch(query, offset, limit, suggest) map response
      case _ =>
        Future.successful(
          BadRequest(Json.obj("status" -> 400, "code" -> "missing_query", "message_en" -> "No query specified."))
        )
    }
  }

}
