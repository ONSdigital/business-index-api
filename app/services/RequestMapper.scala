package services

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse

/**
 * Created by coolit on 03/05/2018.
 */
trait RequestMapper[A] {
  def fromBusinessListResponse(resp: RequestSuccess[SearchResponse]): Option[List[A]]
  def fromBusinessResponse(resp: RequestSuccess[SearchResponse]): Option[A]
}
