package controllers.v1

import javax.inject._

import controllers.WebJarAssets
import scala.db.{BusinessDbProvider, FeedbackEntry}
import play.api.mvc._

import scala.concurrent.Future
import com.outworkers.phantom.dsl.context

@Singleton
class FeedbackController @Inject()(webJarAssets: WebJarAssets) extends Controller with BusinessDbProvider {
  def addFeedback(): Action[AnyContent] = Action.async { implicit req =>
    req.body.asJson match {
      case Some(json) => database.feedbackEntries.store(json.as[FeedbackEntry]) map (_ => Ok("Successfully added entry"))
      case None => Future.successful(BadRequest("Invalid JSON payload"))
    }
  }
}
