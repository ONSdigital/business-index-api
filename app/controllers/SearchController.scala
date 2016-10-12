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
  implicit val businessHitFormat: OFormat[Business]  = Json.format[Business]
  implicit val b = Writes.list[Business]
}

@Singleton
class SearchController @Inject()(actorSystem: ActorSystem, client: ElasticClient)(implicit exec: ExecutionContext)
  extends Controller with ElasticDsl with StrictLogging {

  implicit object BusinessHitAs extends HitAs[Business] {
    override def as(hit: RichSearchHit): Business = {
      Business(
        hit.sourceAsMap("id").toString.toLong,
        hit.sourceAsMap("businessName").toString,
        hit.sourceAsMap("uprn").toString.toLong,
        hit.sourceAsMap("industryCode").toString.toLong,
        hit.sourceAsMap("legalStatus").toString.toInt,
        hit.sourceAsMap("tradingStatus").toString.toInt,
        hit.sourceAsMap("turnover").toString,
        hit.sourceAsMap("employmentBands").toString
      )
    }
  }

  def searchBusiness = Action.async { implicit req =>
    val start = Try(req.getQueryString("start").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(req.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    try {
      client.execute {
        search
          .in("bi" / "businesses")
          .query(req.getQueryString("query").get).start(start).limit(limit)
      }.map(response => Ok(response.as[Business]))
    } catch {
      case NonFatal(e) =>
        logger.error("Error connecting to Elasticsearch", e)
        Future(InternalServerError("Error connecting to Elasticsearch."))
    }
  }
}