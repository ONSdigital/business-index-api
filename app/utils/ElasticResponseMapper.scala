package utils

import com.sksamuel.elastic4s.http.search.SearchHit
import models.Business
import services.ResponseMapper

class ElasticResponseMapper extends ResponseMapper {
  def fromSearchHit(hit: SearchHit): Business = Business.fromMap(hit.id.toLong, hit.sourceAsMap)
}
