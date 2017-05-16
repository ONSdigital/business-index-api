package controllers.v1.event

import java.io.File
import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.typesafe.config.Config
import controllers.v1.BusinessIndexObj._
import controllers.v1.{ElasticUtils, SearchControllerUtils}
import io.swagger.annotations.{Api, ApiOperation}
import nl.grons.metrics.scala.DefaultInstrumented
import org.slf4j.LoggerFactory
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc._
import services.store.{DeleteCommand, EventCommand, EventStore, StoreCommand}
import uk.gov.ons.bi.models.BusinessIndexRec
import uk.gov.ons.bi.{CsvProcessor, Utils}

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Volodymyr.Glushak on 06/04/2017.
  */
@Api("Modify")
@Singleton
class PutController @Inject()(elastic: ElasticClient, val config: Config)(
  implicit context: ExecutionContext
) extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with ElasticUtils with EventStore {

  private[this] val EVENT_APPENDER = "OVERLOAD_LOG"
  private[this] val eventStorage = LoggerFactory.getLogger(EVENT_APPENDER)

  def deleteById(businessId: String): Action[AnyContent] = Action.async {
    errAsResponse {
      deleteByIdImpl(businessId).map(x => Ok(x.toString))
    }
  }

  private[this] def deleteByIdImpl(businessId: String, logInHBase: Boolean = true) = {
    logger.debug(s"Delete request for id: $businessId")
    elastic.execute {
      delete id businessId.toLong from indexName
    } map { r =>
      val bir = BusinessIndexRec(businessId.toLong, "", None, None, None, None, None, None, None, None, None, None)
      val event = s"""DELETE,${bir.toCsv}"""
      eventStorage.info(event)
      if (logInHBase) storeEvent(EventCommand(bir, DeleteCommand))
      OpStatus.opDelete(businessId, r.isFound)
    }
  }

  def store: Action[AnyContent] = Action.async { implicit request =>
    errAsResponse {
      logger.debug(s"Store requested ${request.body}")
      val json = request.body match {
        case AnyContentAsRaw(raw) => Json.parse(raw.asBytes().getOrElse(sys.error("Invalid or empty input")).utf8String)
        case AnyContentAsText(text) => Json.parse(text)
        case AnyContentAsJson(jsonStr) => jsonStr
        case _ => sys.error(s"Unsupported input type ${request.body}")
      }
      storeImpl(biFromJson(json)).map(x => Ok(x.toString))
    }
  }

  def eventLog = Action {
    val header = "\"COMMNAD\"," + BusinessIndexRec.cBiSecuredHeader + "\n"
    Ok(header + getAll.map(_.event.toCsv).mkString("\n"))
  }

  private[this] def storeImpl(bir: BusinessIndexRec, logInHbase: Boolean = true) = {
    elastic execute {
      index into indexName id bir.id fields BusinessIndexRec.toMap(bir)
    } map { r =>
      import OpStatus._
      val event = s"""STORE,${bir.toCsv}"""
      eventStorage.info(event)
      if (logInHbase) storeEvent(EventCommand(bir, StoreCommand))
      if (r.getVersion > 1) opUpdate(bir.id.toString, success = true) else opCreate(bir.id.toString, success = true)
    }
  }

  // public api
  @ApiOperation(value = "Import event history changes to searching",
    notes = "gets changes from hbase and replaces existing ones",
    httpMethod = "GET")
  def apply = Action {
    logger.debug(s"Request received to import HBase event records into Elastic")
    val data = getAll
    data.foreach { ec =>
      ec.command match {
        case DeleteCommand => deleteByIdImpl(ec.event.id.toString, logInHBase = false)
        case StoreCommand => storeImpl(ec.event, logInHbase = false)
      }
    }

    Ok(s"Elastic updated with ${data.length} records.")

  }

  // re-apply event history
  // reapply the edit history stored in HBASE after new data ingestion run has been executed

  // function apply -> that will perform change to elastic index based on what is stored in HBase
  // so in essence do bulkUpdate but read the data from HBase not from file
  // you will need to replace changes that are in elasticsearch in accordance to changes in
  // hbase

  def bulkUpdate: Action[MultipartFormData[TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val fs = request.body.files.flatMap { file => {
      logger.debug(s"Read file with instructions: ${file.filename}")
      val outFile = s"${System.getProperty("java.io.tmpdir")}/${System.currentTimeMillis()}_${file.filename}"
      val workFile = file.ref.moveTo(new File(outFile))
      CsvProcessor.csvToMap(Utils.readFile(workFile.getAbsolutePath)).map { rec =>
        val id = rec.map { case (k, v) => k.toUpperCase -> v }.getOrElse("ID", sys.error("ID column not found."))
        rec("COMMAND") match {
          case "DELETE" => deleteByIdImpl(id)
          case "STORE" | "PUT" => storeImpl(BusinessIndexRec.fromMap(id.toLong, rec))
          case x => sys.error(s"Unknown command $x in file ${file.filename}")
        }
      }
    }
    }
    Future.sequence(fs).map { seqRes =>
      Ok(s"[${seqRes.mkString(",")}]")
    }
  }

  override protected def tableName: String = config.getString("hbase.events.table.name")
}

case class OpStatus(id: String, clazz: String, success: Boolean) {
  override def toString: String = OpStatus.opToJson(this).toString()
}

object OpStatus {
  implicit val opStatusFormat: OFormat[OpStatus] = Json.format[OpStatus]

  def opFromJson(x: String): OpStatus = Json.fromJson[OpStatus](Json.parse(x)) match {
    case JsSuccess(xv, _) => xv
    case JsError(err) => sys.error(s"Can not parse op status json $x -> $err")
  }

  def opListFromJson(x: String): List[OpStatus] = Json.fromJson[List[OpStatus]](Json.parse(x)) match {
    case JsSuccess(xv, _) => xv
    case JsError(err) => sys.error(s"Can not parse op status json $x -> $err")
  }

  def opToJson(ob: OpStatus): JsValue = Json.toJson[OpStatus](ob)

  def opDelete(id: String, success: Boolean) = OpStatus(id, "delete", success)

  def opCreate(id: String, success: Boolean) = OpStatus(id, "create", success)

  def opUpdate(id: String, success: Boolean) = OpStatus(id, "update", success)
}
