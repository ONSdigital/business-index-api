package controllers.v1

import javax.inject._

import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.StrictLogging
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
  * @param exec
  */
@Singleton
class SearchController @Inject()(environment: Environment, elasticsearchClient: ElasticClient)(implicit exec: ExecutionContext)
  extends Controller with ElasticDsl with StrictLogging {

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

  def searchBusiness(suggest: Boolean) = Action.async { implicit request =>
    val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    request.getQueryString("q").orElse(request.getQueryString("query")) match {
      case Some(query) if query.length > 0 =>
        // if suggest, match on the BusinessName only, else assume it's an Elasticsearch query
        val definition = if (suggest) matchQuery("BusinessName", query) else QueryStringQueryDefinition(query)
        elasticsearchClient.execute {
          search.in(s"bi-${environment.mode.toString.toLowerCase}" / "business")
            .query(definition)
            .start(offset)
            .limit(limit)
        }.map { elasticsearchResponse =>
          elasticsearchResponse.as[Business] match {
            case businesses if businesses.length > 0 =>
              Ok(Json.toJson(businesses))
                .withHeaders(
                  "X-Total-Count" -> elasticsearchResponse.totalHits.toString,
                  "X-Max-Score" -> elasticsearchResponse.maxScore.toString
                )
            case _ => Ok("{}").as(JSON)
          }
        }.recover {
          case e: NoNodeAvailableException => ServiceUnavailable(Json.obj("status" -> 503, "code" -> "es_down", "message_en" -> e.getMessage))
          case NonFatal(e) => InternalServerError(Json.obj("status" -> 500, "code" -> "internal_error", "message_en" -> e.getMessage))
        }
      case _ =>
        Future.successful(BadRequest(Json.obj("status" -> 400, "code" -> "missing_query", "message_en" -> "No query specified.")))
    }
  }
}
