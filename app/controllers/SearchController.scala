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
class SearchController @Inject()(actorSystem: ActorSystem, client: ElasticClient)(implicit exec: ExecutionContext)
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

  def searchBusiness = Action.async { implicit req =>
    val start = Try(req.getQueryString("start").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(req.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    try {
      client.execute {
        search
          .in("bi" / "business")
          .query(req.getQueryString("query").get).start(start).limit(limit)
      }.map(queryResponse => Ok(Json.toJson(queryResponse.as[Business])))
    } catch {
      case NonFatal(e) =>
        logger.error("Error connecting to Elasticsearch", e)
        Future(InternalServerError("Error connecting to Elasticsearch."))
    }
  }
}