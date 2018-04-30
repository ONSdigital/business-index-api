package controllers.v1

//import com.sksamuel.elastic4s.RichGetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.typesafe.scalalogging.StrictLogging
//import org.elasticsearch.ElasticsearchException
//import org.elasticsearch.client.transport.NoNodeAvailableException
import play.api.libs.json.Json
import play.api.mvc.{ Controller, Result }
//import services.SearchResponse
import uk.gov.ons.bi.models.BusinessIndexRec
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }

trait SearchControllerUtils extends Controller {

  @tailrec
  final protected def buildErrMsg(x: Throwable, msgs: List[String] = Nil): String = Option(x.getCause) match {
    case None => (x.getMessage :: msgs).reverse.mkString(" ")
    case Some(ex) => buildErrMsg(ex, x.getMessage :: msgs)
  }

  //  private[this] def isElasticFailed(ex: Throwable): Boolean = Option(ex.getCause).exists {
  //    case _: ElasticsearchException => true
  //    case cc => isElasticFailed(cc)
  //  }

  protected[this] def responseRecover(query: String, failOnQueryError: Boolean): PartialFunction[Throwable, Result] = {
    //    case e: NoNodeAvailableException => ServiceUnavailable
    //    case e: RuntimeException if isElasticFailed(e) =>
    //      if (failOnQueryError) InternalServerError else Ok(s"""{"queryerror":"ES could not execute query","query":"$query"}""")
    case NonFatal(e) =>
      //      logger.error(s"Internal error ${e.getMessage}", e)
      InternalServerError
    case _ => InternalServerError
  }

  // response wrapping with extra headers
  //case class SearchData(totalHits: Long, maxScore: Float, businesses:Seq[BusinessIndexRec])

  //  protected def response(data: SearchResponse): Result = responseWithHTTPHeaders(data, Ok(Json.toJson(data.businesses.map(_.secured))))
  //
  //  protected def responseWithHTTPHeaders(resp: SearchResponse, searchResult: Result): Result = {
  //    searchResult.withHeaders(
  //      "X-Total-Count" -> resp.totalHits.toString,
  //      "X-Max-Score" -> resp.maxScore.toString
  //    )
  //  }

  //    protected[this] def resultAsBusiness(businessId: Long, resp: SearchResponse): Option[BusinessIndexRec] = Try(
  //      BusinessIndexRec.fromMap(businessId, Option(resp.source).map(
  //        _.asScala.toMap[String, AnyRef]
  //      ).getOrElse(sys.error("no data")))
  //    ).toOption

  protected[this] def errAsResponse(f: => Future[Result]): Future[Result] = Try(f) match {
    case Success(g) => g
    case Failure(err) =>
      //      logger.error("Unable to produce response.", err)
      Future.successful {
        InternalServerError(s"{err = '${buildErrMsg(err)}'}")
      }
  }
}
