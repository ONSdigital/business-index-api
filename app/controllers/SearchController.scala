package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, Promise}
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.source.Indexable
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.action.get.GetResponse

import scala.util.Try
import scala.util.control.NonFatal
import play.api.libs.json._

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
class SearchController @Inject()(actorSystem: ActorSystem, elasticSearch: ElasticClient)(implicit exec: ExecutionContext)
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
        }.map(elasticSearchResponse =>
          Ok(Json.toJson(elasticSearchResponse.as[Business]))
        )
      case _ =>
        Future(BadRequest(Json.obj("status" -> "400", "code" -> "missing_query", "message_en" -> "No query specified.")))
    }
  }
}