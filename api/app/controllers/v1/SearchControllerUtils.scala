package controllers.v1

import com.outworkers.util.catsparsers.{parse => cparse}
import com.sksamuel.elastic4s.RichGetResponse
import com.typesafe.scalalogging.StrictLogging
import controllers.v1.BusinessIndexObj._
import org.elasticsearch.client.transport.NoNodeAvailableException
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Created by Volodymyr.Glushak on 22/03/2017.
  */
trait SearchControllerUtils extends Controller with StrictLogging {

  protected[this] def responseRecover: PartialFunction[Throwable, Result] = {
    case e: NoNodeAvailableException => ServiceUnavailable(
      Json.obj(
        "status" -> 503,
        "code" -> "es_down",
        "message_en" -> e.getMessage
      )
    )
    case NonFatal(e) =>
      logger.error(s"Internal error ${e.getMessage}", e)
      InternalServerError(
        Json.obj(
          "status" -> 500,
          "code" -> "internal_error",
          "message_en" -> e.getMessage
        )
      )
  }

  // response wrapping with extra headers
  case class SearchData(totalHits: Long, maxScore: Float)

  protected def response(tp: (SearchData, List[BusinessIndexRec])): Result = {
    val (resp, businesses) = tp
    businesses match {
      case _ :: _ => responseWithHTTPHeaders(resp, Ok(Json.toJson(businesses.map(_.secured))))
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

}
