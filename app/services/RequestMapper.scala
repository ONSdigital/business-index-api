package services

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse

trait RequestMapper[A] {
  def fromBusinessListResponse(resp: RequestSuccess[SearchResponse]): Option[List[A]]
  def fromBusinessResponse(resp: RequestSuccess[SearchResponse]): Option[A]
}
