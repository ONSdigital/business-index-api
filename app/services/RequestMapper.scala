package services

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse

trait ResponseMapper[A] {
  def fromBusinessSeqResponse(resp: RequestSuccess[SearchResponse]): Seq[A]
  def fromBusinessResponse(resp: RequestSuccess[SearchResponse]): Option[A]
}
