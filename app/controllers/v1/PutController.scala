package controllers.v1

import java.io.File
import javax.inject.{ Inject, Singleton }

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.FileAppender
import com.sksamuel.elastic4s.{ ElasticClient, ElasticDsl }
import com.typesafe.config.Config
import io.swagger.annotations.Api
import nl.grons.metrics.scala.DefaultInstrumented
import org.slf4j.LoggerFactory
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc._
import uk.gov.ons.bi.{ CsvProcessor, Utils }
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

/**
 * Created by Volodymyr.Glushak on 06/04/2017.
 */
@Api("Modify")
@Singleton
class PutController @Inject() (elastic: ElasticClient, val config: Config)(
    implicit
    context: ExecutionContext
) extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with ElasticUtils {

  private[this] val EVENT_APPENDER = "OVERLOAD_LOG"
  private[this] val eventStorage = LoggerFactory.getLogger(EVENT_APPENDER)

  def deleteById(businessId: String): Action[AnyContent] = Action.async {
    errAsResponse {
      deleteByIdImpl(businessId).map(x => Ok(x.toString))
    }
  }

  private[this] def deleteByIdImpl(businessId: String) = {
    logger.debug(s"Delete request for id: $businessId")
    elastic.execute {
      delete id businessId.toLong from indexName
    } map { r =>
      val bir = BusinessIndexRec(businessId.toLong, "", None, None, None, None, None, None, None, None, None, None)
      eventStorage.info(s"""DELETE,${bir.toCsv}""")
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
      json.validate[BusinessIndexRec] match {
        case JsSuccess(bussn, _) => storeImpl(bussn).map(x => Ok(x.toString))
        case JsError(err) => sys.error(s"Unable to parse business index JSON ${json.toString} -> $err")
      }

    }
  }

  def eventLog = Action {
    val z = LoggerFactory.getILoggerFactory match {
      case x: LoggerContext => x.getLoggerList.asScala.flatMap { logger =>
        logger.iteratorForAppenders().asScala.find { appender =>
          appender.getName == EVENT_APPENDER
        }
      }
    }
    logger.debug(s"Found appender[s] for event log $z")
    z.collect {
      case app: FileAppender[_] => app.getFile
    }.map { f =>
      logger.debug(s"Display file $f")
      Utils.readFile(f).mkString("\n")
    }.headOption match {
      case None => Ok("no_data")
      case Some(str) => Ok(str)
    }
  }

  private[this] def storeImpl(bir: BusinessIndexRec) = {
    elastic execute {
      index into indexName id bir.id fields BusinessIndexRec.toMap(bir)
    } map { r =>
      import OpStatus._
      eventStorage.info(s"""STORE,${bir.toCsv}""")
      if (r.getVersion > 1) opUpdate(bir.id.toString, success = true) else opCreate(bir.id.toString, success = true)
    }
  }

  def bulkUpdate: Action[MultipartFormData[TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val fs = request.body.files.flatMap { file =>
      {
        logger.debug(s"Read file with instructions: ${file.filename}")
        val outFile = s"${System.getProperty("java.io.tmpdir")}/${System.currentTimeMillis()}_${file.filename}"
        val workFile = file.ref.moveTo(new File(outFile))
        CsvProcessor.csvToMap(Utils.readFile(workFile.getAbsolutePath)).map { rec =>
          val id = rec.map(e => e._1.toUpperCase -> e._2).getOrElse("ID", sys.error("ID column not found."))
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
