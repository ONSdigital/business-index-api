package controllers.v1

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.FeedbackObj._
import io.swagger.annotations.{Api, ApiOperation}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import play.api.mvc.{Controller, _}
import uk.gov.ons.bi.Utils

import scala.util.control.NonFatal
//import services.MailAgent


/**
  *
  * @param config
  */
@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends Controller {
  private[this] val logger = LoggerFactory.getLogger(getClass)
  //  private[this] val mailObj = new MailAgent()


  // public api
  @ApiOperation(value = "Send feedback via email",
    notes = "Parses input and formats to a feedback object and the sends it as email",
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
        store(feedbackObj)
      }
      case JsError(err) =>
        logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
        BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
    }

  }


  def store(feedbackObj: FeedbackObj): Result = {
    logger.debug(s"Feedback Received: $feedbackObj")

    Ok("")
  }

}

case class FeedbackObj(id: Option[Int], username: String, name: String, date: String, subject: String, ubrn: Option[List[Long]], query: Option[String], comments: String, hide_status: Option[Boolean])

object FeedbackObj {

  def toMap(o: FeedbackObj) = Map(
    "username" -> o.username,
    "name" -> o.name,
    "date" -> o.date,
    "subject" -> o.subject,
    "comments" -> o.comments
  ) ++
    o.ubrn.map(v => "ubrn" -> v.mkString(",")).toMap ++
    o.query.map(v => "query" -> v).toMap ++
    o.id.map(v => "id" -> v).toMap ++
    o.hide_status.map(v => "hide status" -> v).toMap


  def fromMap(values: Map[String, String]) =
    FeedbackObj(values.get("id").map(_.toInt), values("username"), values("name"), values("date"), values("subject"),
      values.get("ubrn").map(_.split(",").map(_.toLong).toList),
      values.get("query"), values("comments"), values.get("hide_status").map(_.toBoolean))


  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]
}