package repository

import java.util.concurrent.TimeoutException
import javax.inject.Inject

import com.sksamuel.elastic4s.http._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ HttpClient, RequestSuccess }
import com.sksamuel.elastic4s.searches.SearchDefinition
import models._
import services.BusinessService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by coolit on 03/05/2018.
 */
class ElasticSearchBusinessRepository @Inject() (elastic: HttpClient) extends BusinessService with ElasticDsl {

  def findBusiness(query: SearchDefinition): Future[Either[ErrorMessage, Option[List[BusinessIndexRec]]]] = {
    elastic.execute(query).map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(BusinessIndexRec.fromRequestSuccessSearch(r))
      case Left(f: RequestFailure) => handleRequestFailure[List[BusinessIndexRec]](f)
    } recover elasticSearchRecover[List[BusinessIndexRec]]
  }

  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[BusinessIndexRec]]] = {
    elastic.execute {
      search("bi-dev").matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(BusinessIndexRec.fromRequestSuccessId(r))
      case Left(f: RequestFailure) => handleRequestFailure[BusinessIndexRec](f)
    } recover elasticSearchRecover[BusinessIndexRec]
  }

  def elasticSearchRecover[T]: PartialFunction[Throwable, Either[ErrorMessage, Option[T]]] = {
    case j: JavaClientExceptionWrapper => Left(ServiceUnavailable(s"ElasticSearch is not available: ${j.getMessage}"))
    case t: TimeoutException => Left(GatewayTimeout(s"Gateway Timeout: ${t.getMessage}"))
    case ex => Left(InternalServerError(s"Internal Server Error: ${ex.getMessage}"))
  }

  def handleRequestFailure[T](f: RequestFailure): Either[ErrorMessage, Option[T]] = {
    logger.error(s"Request to ElasticSearch has failed [${f.error.reason}]")
    Left(InternalServerError(s"Request to ElasticSearch failed: ${f.error.reason}"))
  }
}
