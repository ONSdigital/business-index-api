package repository

import java.util.concurrent.TimeoutException
import javax.inject.Inject

import com.sksamuel.elastic4s.http._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ HttpClient, RequestSuccess }
import com.sksamuel.elastic4s.searches.queries.QueryStringQueryDefinition
import config.ElasticSearchConfig
import models._
import play.api.mvc.{ AnyContent, Request }
import services.BusinessRepository
import utils.ElasticResponseMapper

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ElasticSearchBusinessRepository @Inject() (
    elastic: HttpClient,
    requestMapper: ElasticResponseMapper,
    config: ElasticSearchConfig
) extends BusinessRepository with ElasticDsl {

  def findBusiness(query: String, request: Request[AnyContent]): Future[Either[ErrorMessage, Seq[Business]]] = {
    val searchRequest = BusinessSearchRequest(query, request)
    val definition = QueryStringQueryDefinition(searchRequest.term).defaultOperator(searchRequest.defaultOperator)
    val searchQuery = search(config.index).query(definition).start(searchRequest.offset).limit(searchRequest.limit)
    logger.debug(s"Executing ElasticSearch query to find businesses with query [$query]")
    elastic.execute(searchQuery).map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(requestMapper.fromBusinessSeqResponse(r))
      case Left(f: RequestFailure) => handleRequestFailure[Seq[Business]](f)
    } recover withTranslationOfFailureToError[Seq[Business]]
  }

  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]] = {
    logger.debug(s"Executing ElasticSearch query to find business by id with id [${id.toString}]")
    elastic.execute {
      search(config.index).matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(requestMapper.fromBusinessResponse(r))
      case Left(f: RequestFailure) => handleRequestFailure[Option[Business]](f)
    } recover withTranslationOfFailureToError[Option[Business]]
  }

  private def withTranslationOfFailureToError[B] = new PartialFunction[Throwable, Either[ErrorMessage, B]] {
    override def isDefinedAt(cause: Throwable): Boolean = true

    override def apply(cause: Throwable): Either[ErrorMessage, B] = {
      logger.error(s"Recovering from ElasticSearch failure [$cause].")
      cause match {
        case j: JavaClientExceptionWrapper => Left(ServiceUnavailable(s"ElasticSearch is not available: ${j.getMessage}"))
        case t: TimeoutException => Left(GatewayTimeout(s"Gateway Timeout: ${t.getMessage}"))
        case ex => Left(InternalServerError(s"Internal Server Error: ${ex.getMessage}"))
      }
    }
  }

  def handleRequestFailure[T](f: RequestFailure): Either[ErrorMessage, T] = {
    logger.error(s"Request to ElasticSearch has failed [${f.error.reason}]")
    Left(InternalServerError(s"Request to ElasticSearch failed: ${f.error.reason}"))
  }
}
