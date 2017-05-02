package controllers.v1

import com.sksamuel.elastic4s.RichGetResponse
import com.typesafe.scalalogging.StrictLogging
import controllers.v1.BusinessIndexObj._
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.transport.NoNodeAvailableException
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Controller, Result}
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Created by Volodymyr.Glushak on 22/03/2017.
  */
trait SearchControllerUtils extends Controller with StrictLogging {

  @tailrec
  final protected def buildErrMsg(x: Throwable, msgs: List[String] = Nil): String = {
    Option(x.getCause) match {
      case None => (x.getMessage :: msgs).reverse.mkString(" ")
      case Some(ex) => buildErrMsg(ex, x.getMessage :: msgs)
    }
  }

  protected def errAsJson(status: Int, code: String, msg: String): JsObject = {
    Json.obj(
      "status" -> status,
      "code" -> code,
      "message_en" -> msg
    )
  }

  private[this] def isElasticFailed(ex: Throwable): Boolean = Option(ex.getCause).exists {
    case _: ElasticsearchException => true
    case cc => isElasticFailed(cc)
  }

  protected[this] def responseRecover(query: String, failOnQueryError: Boolean): PartialFunction[Throwable, Result] = {
    case e: NoNodeAvailableException => ServiceUnavailable(errAsJson(503, "es_down", buildErrMsg(e)))
    case e: RuntimeException if isElasticFailed(e) =>
      def err(txt: String) = errAsJson(500, txt, s"Can not perform search for request: $query")
      if (failOnQueryError) InternalServerError(err("query_error")) else Ok(err("query_warn"))
    case NonFatal(e) =>
      logger.error(s"Internal error ${e.getMessage}", e)
      InternalServerError(errAsJson(500, "internal_error", buildErrMsg(e)))
  }

  // response wrapping with extra headers
  case class SearchData(totalHits: Long, maxScore: Float)

  protected def response(tp: (SearchData, List[BusinessIndexRec])): Result = {
    val (resp, businesses) = tp
    businesses match {
      case _ :: _ => responseWithHTTPHeaders(resp, Ok(biListToJson(businesses.map(_.secured))))
      case _ => responseWithHTTPHeaders(resp, Ok("{}").as(JSON))
    }
  }

  protected def responseWithHTTPHeaders(resp: SearchData, searchResult: Result): Result = {
    searchResult.withHeaders(
      "X-Total-Count" -> resp.totalHits.toString,
      "X-Max-Score" -> resp.maxScore.toString)
  }

  protected[this] def resultAsBusiness(businessId: Long, resp: RichGetResponse): Option[BusinessIndexRec] = Try(
    BusinessIndexRec.fromMap(businessId, Option(resp.source).map(
      _.asScala.toMap[String, AnyRef]
    ).getOrElse(Map.empty[String, AnyRef]))).toOption

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${buildErrMsg(err)}'}")
      }
  }

}
