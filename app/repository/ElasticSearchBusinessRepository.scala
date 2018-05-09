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
import services.BusinessService
import utils.ElasticRequestMapper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ElasticSearchBusinessRepository @Inject() (elastic: HttpClient, requestMapper: ElasticRequestMapper, config: ElasticSearchConfig)
    extends BusinessService with ElasticDsl {

  def findBusiness(query: String, request: Request[AnyContent]): Future[Either[ErrorMessage, Seq[Business]]] = {
    val searchRequest = BusinessSearchRequest(query, request)
    val definition = QueryStringQueryDefinition(searchRequest.term).defaultOperator(searchRequest.defaultOperator)
    val searchQuery = search(config.index).query(definition).start(searchRequest.offset).limit(searchRequest.limit)
    elastic.execute(searchQuery).map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(requestMapper.fromBusinessSeqResponse(r))
      case Left(f: RequestFailure) => handleRequestFailure[Seq[Business]](f)
    } recover elasticSearchRecover[Seq[Business]]
  }

  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]] = {
    elastic.execute {
      search(config.index).matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(requestMapper.fromBusinessResponse(r))
      case Left(f: RequestFailure) => handleRequestFailure[Option[Business]](f)
    } recover elasticSearchRecover[Option[Business]]
  }

  def elasticSearchRecover[T]: PartialFunction[Throwable, Either[ErrorMessage, T]] = {
    case j: JavaClientExceptionWrapper => Left(ServiceUnavailable(s"ElasticSearch is not available: ${j.getMessage}"))
    case t: TimeoutException => Left(GatewayTimeout(s"Gateway Timeout: ${t.getMessage}"))
    case ex => Left(InternalServerError(s"Internal Server Error: ${ex.getMessage}"))
  }

  def handleRequestFailure[T](f: RequestFailure): Either[ErrorMessage, T] = {
    logger.error(s"Request to ElasticSearch has failed [${f.error.reason}]")
    Left(InternalServerError(s"Request to ElasticSearch failed: ${f.error.reason}"))
  }
}
