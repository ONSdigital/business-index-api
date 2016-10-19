package controllers.v1

import javax.inject._

import com.sksamuel.elastic4s._
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class Business(id: Long,
                    businessName: String,
                    uprn: Long,
                    industryCode: Long,
                    legalStatus: Int,
                    tradingStatus: Int,
                    turnover: String,
                    employmentBands: String)

object Business {
  implicit val businessHitFormat = Json.format[Business]
}

@Singleton
class SearchController @Inject()(elasticSearch: ElasticClient)(implicit exec: ExecutionContext)
  extends Controller with ElasticDsl with StrictLogging {

  implicit object BusinessHitAs extends HitAs[Business] {
    override def as(hit: RichSearchHit): Business = {
      Business(
        hit.id.toLong,
        hit.sourceAsMap("BusinessName").toString,
        hit.sourceAsMap("UPRN").toString.toLong,
        hit.sourceAsMap("IndustryCode").toString.toLong,
        hit.sourceAsMap("LegalStatus").toString.toInt,
        hit.sourceAsMap("TradingStatus").toString.toInt,
        hit.sourceAsMap("Turnover").toString,
        hit.sourceAsMap("EmploymentBands").toString
      )
    }
  }

  def searchBusiness = Action.async { implicit request =>
    val start = Try(request.getQueryString("start").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    request.getQueryString("q").orElse(request.getQueryString("query")) match {
      case Some(query) if query.length > 0 =>
        elasticSearch.execute {
          search.in("bi" / "business")
            .query(query)
            .start(start)
            .limit(limit)
        }.map { elasticSearchResponse =>
          val businesses = elasticSearchResponse.as[Business]
          if (businesses.length > 0) Ok(Json.toJson(businesses))
          else NoContent
        }
      case _ =>
        Future.successful(BadRequest(Json.obj("status" -> "400", "code" -> "missing_query", "message_en" -> "No query specified.")))
    }
  }
}
