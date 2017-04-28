package controllers.v1

import javax.inject.Inject

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import io.swagger.annotations.Api
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import play.api.mvc.{Controller, _}
import services.MailAgent
import uk.gov.ons.bi.ingest.helper.Utils.configOverride


@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends Controller {
  private[this] val logger = LoggerFactory.getLogger(getClass)
  private[this] val mailObj = new MailAgent()


  import FeedbackObj._

  def feedback = Action { request =>
    // check if its json if so do json
    // else: if its text then convert it to json
    // else show error message
    val json = request.body.asJson.get
    val feedbackObj = Json.fromJson[FeedbackObj](json) match {
      case JsSuccess(x, _) => x
      case JsError(err) => sys.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
    }

    logger.debug(s"Feedback Received: $feedbackObj")

    val firstLine = feedbackObj.ubrn.map { ubrn => s"${feedbackObj.subject} with UBRN: $ubrn" }.getOrElse(feedbackObj.subject)
    val subject = s"[${feedbackObj.subject}] Feedback About Business Index From ${feedbackObj.name} at ${feedbackObj.date}"

    val content =
      s"""
         | $firstLine
         | ${feedbackObj.query.map(q => s"Query: $q").getOrElse("")}
         |
         | ${feedbackObj.comments}
         |
      """.stripMargin

    val response =
      s"""
         |Email with subject: $subject
         |${feedbackObj.query.map(q => s"with query of $q").getOrElse("")}
         |${feedbackObj.ubrn.map(u => s"and with UBRN of $u").getOrElse("")}
       """.stripMargin

    if (configOverride("email.service.enabled").toBoolean) {
      mailObj.sendMessage(
        subject = subject,
        content = content,
        from = configOverride("feedback.email.from"),
        to = configOverride("feedback.email.to"))
      feedbackObj.ubrn.map { urbn => urbn}
      Ok (s"Email with subject: $response")
    } else {
      Ok (s"Email Server Is Disable! Feedback wasn't sent for $response")
    }

  }
}


case class FeedbackObj(username: String, name: String, date: String, subject: String, ubrn: Option[Long], query: Option[String], comments: String)

object FeedbackObj {
  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]
}