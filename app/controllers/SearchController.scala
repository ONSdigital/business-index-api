package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, Promise}
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.typesafe.scalalogging.StrictLogging

import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class SearchController @Inject()(actorSystem: ActorSystem, client: ElasticClient)(implicit exec: ExecutionContext)
  extends Controller with ElasticDsl with StrictLogging {

  def searchBusiness = Action.async { implicit req =>
    val start = Try(req.getQueryString("start").getOrElse("0").toInt).getOrElse(0)
    val limit = Try(req.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)

    try {
      client.execute {
        search
          .in("bi" / "businesses")
          .query(req.getQueryString("query").get).start(start).limit(limit)
      }.map (response => Ok(response.toString))
    } catch {
      case NonFatal(e) =>
        logger.error("Error connecting to Elasticsearch", e)
        Future(InternalServerError("Error connecting to Elasticsearch."))
    }
  }
}