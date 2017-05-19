package controllers.v1.feedback

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.feedback.FeedbackObj._
import io.swagger.annotations._
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
  @ApiOperation(value = "Submit feedback",
    notes = "Parses input and formats to a feedback object to store",
    responseContainer = "String",
    httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Create a new feedback",
      required = true,
      dataType = "controllers.FeedbackObj", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - New feedback has been stored."),
    new ApiResponse(code = 400, message = "Client Side Error - Not input given/ found."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found.")))
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
  @ApiOperation(value = "Update (single) progress status field of a given feedback record",
    notes = "Field progressStatus is the only single column changed (New/ In Progress/ Completed)",
    httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Create a new feedback",
      required = true,
      dataType = "controllers.FeedbackObj", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - 'progressStatus' has been successfully been modified."),
    new ApiResponse(code = 400, message = "Client Side Error - Not input given/ found."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found.")))
  def updateProgress = Action { request =>
    val json = validate(request)

    withError {
      Json.fromJson[FeedbackObj](json) match {
        case JsSuccess(feedbackObj, _) =>
          logger.debug(s"Updated Feedback Received: $feedbackObj")
          val updated = progress(feedbackObj)
          Ok(s""" { "id" : "${updated.id.getOrElse("")}", "progressStatus": "${updated.progressStatus.getOrElse("")}" } """)
        case JsError(err) =>
          logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
          BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
      }
    }
  }

  // public api
  @ApiOperation(value = "Delete a feedback record from HBase",
    hidden = true,
    notes = "Hard delete - This will get rid of the entire record in HBase contrary to hide.",
    httpMethod = "DELETE")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Record successfully deleted."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found.")))
  def deleteFeedback(@ApiParam(value = "hbase record id", required = true) id: String) = Action {
    logger.debug(s"Processing deletion of HBase record: $id")
    withError {
      val res = delete(id)
      Ok(s""" { "id" : "$res" }   """)
    }
  }


  // public api
  @ApiOperation(value = "Toggle (single) feedback visibility status",
    notes = "Soft delete - a substitute for delete that allows to restore the feedback using a boolean paramater in hide function.",
    httpMethod = "DELETE")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Hide Status of respective record toggled."),
    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found.")))
  def hideFeedback(@ApiParam(value = "hbase record id", required = true) id: String) = Action {
    logger.debug(s"Processing deletion of HBase record: $id")
    withError {
      val res = hide(id)
      Ok(s""" { "id" : "$res" }   """)
    }
  }


  // public api
  @ApiOperation(value = "Display all feedback",
    notes = "Displays all records found in HBase while filtering out those with a 'hideStatus' set to true",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays all records in HBase.")))
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

case class FeedbackObj(id: Option[String], @ApiModelProperty(value = "Name of the resource") username: String, @ApiModelProperty(value = "fhdsjfhs of the resource") name: String, date: Option[String] = Some(System.currentTimeMillis().toString), subject: String, ubrn: Option[List[Long]], query: Option[String], comments: String, progressStatus: Option[String] = Some("New"), hideStatus: Option[Boolean] = Some(false))

object FeedbackObj {

  def toMap(o: FeedbackObj, id: String) = Map(
    "id" -> id,
    "username" -> o.username,
    "name" -> o.name,
    "subject" -> o.subject,
    "comments" -> o.comments,
    "date" ->  System.currentTimeMillis().toString,
    "hideStatus" -> false
  ) ++
    o.ubrn.map(v => "ubrn" -> v.mkString(",")).toMap ++
    o.query.map(v => "query" -> v).toMap ++
    o.progressStatus.map(v => "progressStatus" -> v).toMap


  def fromMap(values: Map[String, String]) =
    FeedbackObj(values.get("id"), values("username"), values("name"), values.get("date").map(_.toString), values("subject"),
      values.get("ubrn").map(_.split(",").map(_.toLong).toList),
      values.get("query"), values("comments"), values.get("progressStatus").map(_.toString), values.get("hideStatus").map(_.toBoolean))


  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]

}