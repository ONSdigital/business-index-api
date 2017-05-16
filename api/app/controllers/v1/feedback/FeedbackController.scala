package controllers.v1.feedback

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.feedback.FeedbackObj._
import io.swagger.annotations.{Api, ApiOperation}
import org.joda.time.LocalDateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc.{Controller, _}
import services.store.FeedbackStore

import scala.util.control.NonFatal


/**
  *
  * @param config
  */
@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends Controller with FeedbackStore {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def validate(request: Request[AnyContent]): JsValue = {
    val json = request.body match {
      case AnyContentAsRaw(raw) => Json.parse(raw.asBytes().getOrElse(sys.error("Invalid or empty input")).utf8String)
      case AnyContentAsText(text) => Json.parse(text)
      case AnyContentAsJson(jsonStr) => jsonStr
      case _ => sys.error(s"Unsupported input type ${request.body}")
    }
    json
  }

  // public api
  @ApiOperation(value = "Store Feedback to HBase",
    notes = "Parses input and formats to a feedback object to store",
    httpMethod = "POST")
  def storeFeedback = Action { request =>
    val json = validate(request)

    withError {
      Json.fromJson[FeedbackObj](json) match {
        case JsSuccess(feedbackObj, _) =>
          logger.debug(s"Feedback Received: $feedbackObj")
          val id = store(feedbackObj)
          Ok(s""" {"id": "$id"} """)
        case JsError(err) =>
          logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
          BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
      }
    }
  }


  // public api
  @ApiOperation(value = "Update (single) feedback record of progress status",
    notes = "progressStatus is the only single column changed (New/ In Progress/ Completed)",
    httpMethod = "PUT")
  def updateProgress() = Action { request =>
    val json = validate(request)

    withError {
      Json.fromJson[FeedbackObj](json) match {
        case JsSuccess(feedbackObj, _) =>
          logger.debug(s"Updated Feedback Received: $feedbackObj")
          val updated = progress(feedbackObj)
          Ok(s""" { "id" : "${updated.id}", "progressStatus": "${updated.progressStatus}" } """)
        case JsError(err) =>
          logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
          BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
      }
    }
  }


  // public api
  @ApiOperation(value = "Toggle (single) feedback visibility status",
    notes = "soft delete is produce using id",
    httpMethod = "DELETE")
  def deleteFeedback(id: String) = Action {
    logger.debug(s"Processing deletion of HBase record: $id")
    withError {
      val res = hide(id)
      Ok(s""" { "id" : "$res" }   """)
    }
  }


  // public api
  @ApiOperation(value = "display all feedback",
    notes = "displays only record with hide status false",
    httpMethod = "GET")
  def display = Action {
    logger.debug(s"Request received to display all feedback records [with status hide as FALSE]")
    Ok(Json.toJson[List[FeedbackObj]](getAll()))
  }


  private[this] def withError(f: => Result): Result = {
    try {
      f
    } catch {
      case NonFatal(ex) =>
        val err = Json.obj(
          "status" -> 500,
          "code" -> "internal_error",
          "message_en" -> ex.getMessage
        )
        InternalServerError(err)
    }
  }

  override protected def tableName: String = config.getString("hbase.feedback.table.name")
}

case class FeedbackObj(id: Option[String], username: String, name: String, date: Option[String] = Some(System.currentTimeMillis().toString), subject: String, ubrn: Option[List[Long]], query: Option[String], comments: String, progressStatus: Option[String] = Some("New"), hideStatus: Option[Boolean] = Some(false))

object FeedbackObj {

  def toMap(o: FeedbackObj, id: String) = Map(
    "id" -> id,
    "username" -> o.username,
    "name" -> o.name,
    "subject" -> o.subject,
    "comments" -> o.comments,
    "date" -> new LocalDateTime().toString,
    "hideStatus" -> false
  ) ++
    o.ubrn.map(v => "ubrn" -> v.mkString(",")).toMap ++
    o.query.map(v => "query" -> v).toMap ++
    o.progressStatus.map(v => "progressStatus" -> v).toMap


  def fromMap(values: Map[String, String]) =
    FeedbackObj(values.get("id"), values("username"), values("name"), values.get("date").map(_.toString), values("subject"),
      values.get("ubrn").map(_.split(",").map(_.toLong).toList),
      values.get("query"), values("comments"), values.get("progressStatus"), values.get("hideStatus").map(_.toBoolean))


  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]

}