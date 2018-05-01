package services

import com.sksamuel.elastic4s.http.{ RequestFailure, RequestSuccess }
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.SearchDefinition

import scala.concurrent.Future

trait BusinessService {
  def findBusiness(query: SearchDefinition): Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]
  def findBusinessById(id: Long): Future[Either[RequestFailure, RequestSuccess[SearchResponse]]]
}