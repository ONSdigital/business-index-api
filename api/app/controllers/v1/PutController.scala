package controllers.v1

import javax.inject.{Inject, Singleton}

import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import com.typesafe.config.Config
import controllers.v1.BusinessIndexObj._
import io.swagger.annotations.Api
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by Volodymyr.Glushak on 06/04/2017.
  */
@Api("Modify")
@Singleton
class PutController @Inject()(elastic: ElasticClient, val config: Config)(
  implicit context: ExecutionContext
) extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with ElasticUtils {

  def deleteById(businessId: String): Action[AnyContent] = Action.async {
    logger.debug(s"Delete request for id: $businessId")
    elastic.execute {
      delete id businessId.toLong from indexName
    } map { r =>
      Ok(s"{'deleted'='${r.isFound}'}")
    }
  }

  private[this] def errAsResponse[T](f: => Future[Result]) = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${buildErrMsg(err)}'}")
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

      val bir = Json.fromJson[BusinessIndexRec](json) match {
        case JsSuccess(js, _) => js
        case err => sys.error(s"Unable to parse json. $err")
      }
      elastic execute {
        index into indexName id bir.id fields BusinessIndexRec.toMap(bir)
      } map { r =>
        if (r.getVersion > 1) Ok("{'updated'='true'}") else Ok("{'created'='true'}")
      }


    }
  }

}
