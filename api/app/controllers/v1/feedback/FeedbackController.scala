package controllers.v1.feedback

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.feedback.FeedbackObj._
import io.swagger.annotations._
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc._
import services.store.FeedbackStore
import controllers.v1.SearchControllerUtils
import com.outworkers.util.play._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal


/**
  *
  * @param config
  */
@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends SearchControllerUtils with FeedbackStore {
  private[this] val logger = LoggerFactory.getLogger(getClass)


  def formatter[T](f: FeedbackObj => T, json: JsValue, message: String) : Future[Result] = {
    Json.fromJson[FeedbackObj](json) match {
      case JsSuccess(feedbackObj, _) =>
        logger.debug(s"Feedback Received: $feedbackObj")
        val res = Try(f(feedbackObj))
        res match {
          case Success (res: String) => Created(s""" {"id": "$res"} """).future
          case Success (res: FeedbackObj) => Ok(s""" { "id" : "${res.id.getOrElse("")}", "progressStatus": "${res.progressStatus.getOrElse("")}" } """).future
          case _ => BadRequest(errAsJson(502, "exception failed or timeout connection", message)).future
        }
      case JsError(err) =>
        logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
        BadRequest(errAsJson(400, "invalid_input", s"Invalid Feedback! Please give properly parsable feedback $json -> $err")).future
    }
  }

  // public api
  @ApiOperation(
    value = "Submit feedback",
    notes = "Parses input and formats to a feedback object to store",
    responseContainer = "String",
    httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Create a new feedback",
      required = true,
      dataType = "controllers.v1.feedback.FeedbackObj", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Created - New feedback has been stored."),
    new ApiResponse(code = 400, message = "Client Side Error - Not input given/ found."),
//    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found."),
    new ApiResponse(code = 502, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")))
  def storeFeedback : Action[AnyContent] = Action.async { implicit request =>
    val json = validate(request)
    val message = "Could not perform operation delete record in source - may be caused by connection timeout or a failed to find endpoint."
    formatter(store, json, message)
  }



  // public api
  @ApiOperation(
    value = "Update (single) progress status field of a given feedback record",
    notes = "Field progressStatus is the only single column changed (New/ In Progress/ Completed)",
    httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "Update an existing feedback post.",
      required = true,
      dataType = "controllers.v1.feedback.FeedbackObj", // complete path
      paramType = "body"
    )
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - 'progressStatus' has been successfully been modified."),
    new ApiResponse(code = 400, message = "Client Side Error - Not input given/ found."),
//    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found."),
    new ApiResponse(code = 502, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")))
  def updateProgress : Action[AnyContent] = Action.async { request =>
    val json = validate(request)
    val message = "Could not perform operation delete record - may be caused by connection timeout or a failed to find endpoint."
    formatter[FeedbackObj](progress, json, message)
  }


  // public api
  @ApiOperation(
    value = "Delete a feedback record",
    hidden = true,
    notes = "Hard delete - This will get rid of the entire record in the source contrary to hide.",
    httpMethod = "DELETE")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Record successfully deleted."),
//    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found."),
    new ApiResponse(code = 502, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")))
  def deleteFeedback(@ApiParam(value = "record id", required = true) id: String) : Action[AnyContent] = Action.async {
      logger.debug(s"Processing deletion of record $id in source")
      val res = Try(delete(id))
      res match {
        case Success(res) => Ok(s""" { "id" : "$res" }   """).future
        case Failure(ex) => BadRequest(errAsJson(502, ex.toString, "Could not perform operation delete record - may be caused by connection timeout or a failed to connect to endpoint.")).future
      }
  }


  // public api
  @ApiOperation(
    value = "Hide feedabck record",
    notes = "Soft delete - toggles a single feedback post visibility to true from false. This acts as a substitute for delete that allows to restore the feedback using a boolean paramater in hide function.",
    httpMethod = "DELETE")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Hide Status of respective record toggled."),
//    new ApiResponse(code = 500, responseContainer = "Json", message = "Internal Server Error - Invalid id thereby cannot be found."),
    new ApiResponse(code = 502, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")))
  def hideFeedback(@ApiParam(value = "HBase record identifier", required = true) id: String) : Action[AnyContent] = Action.async {
    logger.debug(s"Processing deletion of HBase record: $id")
      val res = Try(hide(id))
      res match {
        case Success(res) => Ok(s""" { "id" : "$res" }   """).future
        case Failure(ex) => BadRequest(errAsJson(502, ex.toString, "Could not perform operation hide record - may be caused by connection timeout or a failed to connect to endpoint.")).future
      }
  }


  // public api
  @ApiOperation(
    value = "Display all feedback",
    notes = "Displays all records found while filtering out those with a 'hideStatus' set to true",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays all records in source."),
    new ApiResponse(code = 502, responseContainer = "Json", message = "Internal Server Error - Failed to connection or timeout with endpoint.")))
  def display : Action[AnyContent] = Action.async {
    logger.debug(s"Request received to display all feedback records [with status hide as FALSE]")
    val getFeedback = Try(Json.toJson[List[FeedbackObj]](getAll()))
    getFeedback match {
      case Success(res) => Ok(res).future
      case Failure(ex) => BadRequest(errAsJson(502, ex.toString , "Failed to retrieve data - may be caused by connection timeout or a failed to connect to endpoint.")).future
    }
  }

  @deprecated("this method will be removed - use in built try", "1 June - hbase kerberos")
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

case class FeedbackObj(
          @ApiModelProperty(value = "Auto-generated id", required = false, hidden = true) id: Option[String],
          @ApiModelProperty(value = "User's username", dataType = "String", example = "mustermannm", required = true) username: String,
          @ApiModelProperty(value = "User's Full Name", dataType = "String", example = "Max Mustermann", required = true) name: String,
          @ApiModelProperty(value = "Time in Milliseconds", dataType = "String", example = "1487672388", required = false) date: Option[String] = Some(System.currentTimeMillis().toString),
          @ApiModelProperty(value = "Type of Feedback (Data/ Ui)", dataType = "String", example = "Data Issue", required = true) subject: String,
          @ApiModelProperty(value = "List of UBRNs (if applicable)", required = false) ubrn: Option[List[Long]],
          @ApiModelProperty(value = "Related query (if applicable)", dataType = "String", example = "http://localhost:9000/v1/search/_id:86883196", required = false) query: Option[String],
          @ApiModelProperty(value = "Brief description of the problem", dataType = "String", example = "This is just a comment.", required = true) comments: String,
          @ApiModelProperty(value = "Time in Milliseconds", dataType = "String", example = "New", required = true) progressStatus: Option[String] = Some("New"),
          @ApiModelProperty(value = "Toggle for record visibility", dataType = "boolean", required = false, hidden = true) hideStatus: Option[Boolean] = Some(false))
//java.lang.Long
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