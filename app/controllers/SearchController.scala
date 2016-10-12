package controllers

import akka.actor.ActorSystem
import javax.inject._

import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future, Promise}
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.typesafe.scalalogging.StrictLogging

@Singleton
class SearchController @Inject()(actorSystem: ActorSystem, client: ElasticClient)(implicit exec: ExecutionContext)
  extends Controller with ElasticDsl with StrictLogging {

  def searchBusiness = Action.async { implicit req =>
    try {
      client.execute(search.in("bi" / "businesses").query(req.getQueryString("query").get)).map (response => Ok(response.toString))
    } catch {
      case e: Exception =>
        e.printStackTrace()
        logger.error("Error connecting to Elasticsearch", e)
        Future(InternalServerError("Error connecting to Elasticsearch. Is application.conf filled in properly?\n"))
    }
  }
}