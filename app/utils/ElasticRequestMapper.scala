package utils

import com.sksamuel.elastic4s.http.RequestSuccess
import com.sksamuel.elastic4s.http.search.SearchResponse
import models.BusinessIndexRec
import services.RequestMapper

/**
 * Created by coolit on 03/05/2018.
 */
class ElasticRequestMapper extends RequestMapper[BusinessIndexRec] {
  def fromBusinessListResponse(resp: RequestSuccess[SearchResponse]): Option[List[BusinessIndexRec]] =
    resp.result.hits.hits.toList match {
      case Nil => None
      case xs => Some(xs.map(x => BusinessIndexRec.fromMap(x.id.toLong, x.sourceAsMap).secured))
    }

  def fromBusinessResponse(resp: RequestSuccess[SearchResponse]): Option[BusinessIndexRec] =
    resp.result.hits.hits.toList match {
      case Nil => None
      case x :: _ => Some(BusinessIndexRec.fromMap(x.id.toLong, x.sourceAsMap))
    }
}
