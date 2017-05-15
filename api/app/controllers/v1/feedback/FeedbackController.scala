package controllers.v1.feedback

import javax.inject.Inject

import com.typesafe.config.Config
import controllers.v1.feedback.FeedbackObj._
import io.swagger.annotations.{Api, ApiOperation}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import play.api.mvc.{Controller, _}
import services.store.FeedbackStore
import org.joda.time.LocalDateTime


/**
  *
  * @param config
  */
@Api("Feedback")
class FeedbackController @Inject()(implicit val config: Config) extends Controller with FeedbackStore {
  private[this] val logger = LoggerFactory.getLogger(getClass)


  // public api
  @ApiOperation(value = "Store Feedback to HBase",
    notes = "Parses input and formats to a feedback object to store",
    httpMethod = "POST")
  def storeFeedback = Action { request =>
    val json = request.body match {
      case AnyContentAsRaw(raw) => Json.parse(raw.asBytes().getOrElse(sys.error("Invalid or empty input")).utf8String)
      case AnyContentAsText(text) => Json.parse(text)
      case AnyContentAsJson(jsonStr) => jsonStr
      case _ => sys.error(s"Unsupported input type ${request.body}")
    }

    Json.fromJson[FeedbackObj](json) match {
      case JsSuccess(feedbackObj, _) => {
        logger.debug(s"Feedback Received: $feedbackObj")
        println("Received Feedback Object as2: " + feedbackObj + "\t\t Number #1")
        Ok(store(feedbackObj))
      }
      case JsError(err) =>
        logger.error(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
        BadRequest(s"Invalid Feedback! Please give properly parsable feedback $json -> $err")
    }
  }

  // public api
  @ApiOperation(value = "Alters (single) feedback visibility status",
    notes = "soft delete is produce using id",
    httpMethod = "DELETE")
  def deleteFeedback (id: String) = Action  {
    logger.debug(s"Processing deletion of HBase record: $id")
    val res = hide(id)
    Ok(s"The following record has been modified: $res")
  }

  // public api
  @ApiOperation(value = "display all feedback",
    notes = "displays only record with hide status false",
    httpMethod = "GET")
  def display = Action {
    logger.debug(s"Request received to display all feedback records [with status hide as FALSE]")
    val res = getAll()
    Ok(s"HBase has the following stored: $res")
  }

  override protected def tableName: String = config.getString("hbase.feedback.table.name")
}

case class FeedbackObj(id: Option[String], username: String, name: String, date: Option[String] = Some(new LocalDateTime().toString), subject: String, ubrn: Option[List[Long]], query: Option[String], comments: String, hideStatus: Option[Boolean] = Some(false))

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
    o.hideStatus.map(v => "hideStatus" -> v).toMap


  def fromMap(values: Map[String, String]) =
    FeedbackObj(values.get("id"), values("username"), values("name"), values.get("date").map(_.toString), values("subject"),
      values.get("ubrn").map(_.split(",").map(_.toLong).toList),
      values.get("query"), values("comments"), values.get("hideStatus").map(_.toBoolean))


  implicit val feedbackFormatter: OFormat[FeedbackObj] = Json.format[FeedbackObj]
}