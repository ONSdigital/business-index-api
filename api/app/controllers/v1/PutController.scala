package controllers.v1

import java.io.File
import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.typesafe.config.Config
import controllers.v1.BusinessIndexObj._
import io.swagger.annotations.Api
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc._
import uk.gov.ons.bi.ingest.helper.Utils
import uk.gov.ons.bi.ingest.parsers.CsvProcessor
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by Volodymyr.Glushak on 06/04/2017.
  */
@Api("Modify")
@Singleton
class PutController @Inject()(elastic: ElasticClient, val config: Config)(
  implicit context: ExecutionContext
) extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with ElasticUtils {

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

      storeImpl(Json.fromJson[BusinessIndexRec](json) match {
        case JsSuccess(js, _) => js
        case err => sys.error(s"Unable to parse json. $err")
      }).map(x => Ok(x.toString))
    }
  }

  private[this] def storeImpl(bir: BusinessIndexRec) = {
    elastic execute {
      index into indexName id bir.id fields BusinessIndexRec.toMap(bir)
    } map { r =>
      import OpStatus._
      if (r.getVersion > 1) opUpdate(bir.id.toString, success = true) else opCreate(bir.id.toString, success = true)
    }
  }

  def bulkUpdate: Action[MultipartFormData[TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val fs = request.body.files.flatMap { file => {
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
  override def toString: String = OpStatus.toJson(this).toString()
}

object OpStatus {
  implicit val opStatusFormat: OFormat[OpStatus] = Json.format[OpStatus]

  def fromJson(x: String): OpStatus = Json.fromJson[OpStatus](Json.parse(x)) match {
    case JsSuccess(xv, _) => xv
    case JsError(err) => sys.error(s"Can not parse op status json $x -> $err")
  }

  def listFromJson(x: String): List[OpStatus] = Json.fromJson[List[OpStatus]](Json.parse(x)) match {
    case JsSuccess(xv, _) => xv
    case JsError(err) => sys.error(s"Can not parse op status json $x -> $err")
  }

  def toJson(ob: OpStatus): JsValue = Json.toJson[OpStatus](ob)

  def opDelete(id: String, success: Boolean) = OpStatus(id, "delete", success)

  def opCreate(id: String, success: Boolean) = OpStatus(id, "create", success)

  def opUpdate(id: String, success: Boolean) = OpStatus(id, "update", success)
}
