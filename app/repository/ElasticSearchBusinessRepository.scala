package repository

import javax.inject.Inject

import com.sksamuel.elastic4s.http._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{ HttpClient, RequestSuccess }
import com.sksamuel.elastic4s.searches.SearchDefinition
import models.{ BusinessIndexRec, ErrorMessage }
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
      case Left(e: ErrorMessage) => Left(e)
    }
  }

  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[BusinessIndexRec]]] = {
    elastic.execute {
      search("bi-dev").matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => Right(BusinessIndexRec.fromRequestSuccessId(r))
      case Left(e: ErrorMessage) => Left(e)
    }
  }
}
