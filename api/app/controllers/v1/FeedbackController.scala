package controllers.v1

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.FeedbackObj._
import io.swagger.annotations.{Api, ApiOperation}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import play.api.mvc.{Controller, _}
import services.MailAgent
import uk.gov.ons.bi.Utils._


/**
  *
  * @param config
  */
@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends Controller {
  private[this] val logger = LoggerFactory.getLogger(getClass)
  private[this] val mailObj = new MailAgent()


  // public api
  @ApiOperation(value = "Parses feedback and creates object",
    notes = "Parses input and formats to a feedback object",
    httpMethod = "POST")
  def feedback = Action { request =>
    val json = request.body match {
      case AnyContentAsRaw(raw) => Json.parse(raw.asBytes().getOrElse(sys.error("Invalid or empty input")).utf8String)
      case AnyContentAsText(text) => Json.parse(text)
      case AnyContentAsJson(jsonStr) => jsonStr
      case _ => sys.error(s"Unsupported input type ${request.body}")
    }

    Json.fromJson[FeedbackObj](json) match {
      case JsSuccess(feedbackObj, _) => {
        email(feedbackObj)
      }
      case JsError(err) =>
        logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
        BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
    }

  }

  @ApiOperation(value = "Post feedback - composes an email of feedback",
    notes = "Uses feedback object to compose email regarding feedback",
    httpMethod = "POST")
  def email (feedbackObj: FeedbackObj): Result = {
    logger.debug(s"Feedback Received: $feedbackObj")

    val firstLine = feedbackObj.ubrn.map { ubrn => s"${feedbackObj.subject} with UBRN(s): ${ubrn.mkString(", ")}" }.getOrElse(feedbackObj.subject)
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
         |${feedbackObj.ubrn.map(u => s"with UBRN(s) of ${u.mkString(", ")}").getOrElse("")}
         |${feedbackObj.query.map(q => s"and with query of $q").getOrElse("")}
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
      Ok (s"Email Server Is Disabled! Feedback won't be sent for $response")
    }
  }

}


case class FeedbackObj(username: String, name: String, date: String, subject: String, ubrn: Option[List[Long]], query: Option[String], comments: String)

object FeedbackObj {
  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]
}