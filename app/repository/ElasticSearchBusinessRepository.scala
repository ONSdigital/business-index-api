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

/**
 * Created by coolit on 03/05/2018.
 */
class ElasticSearchBusinessRepository @Inject() (elastic: HttpClient, requestMapper: ElasticRequestMapper, config: ElasticSearchConfig)
    extends BusinessService with ElasticDsl {

  def findBusiness(query: String, request: Request[AnyContent]): Future[Either[ErrorMessage, Option[List[Business]]]] = {
    logger.info(s"findBusiness for query [$query]")
    val searchRequest = BusinessSearchRequest(query, request)
    val definition = QueryStringQueryDefinition(searchRequest.term).defaultOperator(searchRequest.defaultOperator)
    val searchQuery = search(config.index).query(definition).start(searchRequest.offset).limit(searchRequest.limit)
    logger.info(s"findBusiness for searchQuery [${searchQuery.toString}]")
    elastic.execute(searchQuery).map {
      case Right(r: RequestSuccess[SearchResponse]) => {
        logger.info(s"RequestSuccess: ${r}")
        Right(requestMapper.fromBusinessListResponse(r))
      }
      case Left(f: RequestFailure) => {
        logger.info(s"RequestFailure: ${f.error.reason}")
        handleRequestFailure[List[Business]](f)
      }
    } recover elasticSearchRecover[List[Business]]
  }

  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]] = {
    logger.info(s"findBusinessById for id [$id]")
    elastic.execute {
      search(config.index).matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => {
        logger.info(s"RequestSuccess: ${r}")
        Right(requestMapper.fromBusinessResponse(r))
      }
      case Left(f: RequestFailure) => {
        logger.info(s"RequestFailure: ${f.error.reason}")
        handleRequestFailure[Business](f)
      }
    } recover elasticSearchRecover[Business]
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
