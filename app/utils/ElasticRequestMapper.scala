package utils

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse
import models.Business
import services.RequestMapper

class ElasticRequestMapper extends RequestMapper[Business] {
  def fromBusinessSeqResponse(resp: RequestSuccess[SearchResponse]): Seq[Business] =
    resp.result.hits.hits.toSeq match {
      case Nil => Seq()
      case xs => xs.map(x => Business.fromMap(x.id.toLong, x.sourceAsMap).secured)
    }

  def fromBusinessResponse(resp: RequestSuccess[SearchResponse]): Option[Business] =
    resp.result.hits.hits.toList match {
      case Nil => None
      case x :: _ => Some(Business.fromMap(x.id.toLong, x.sourceAsMap))
    }
}
