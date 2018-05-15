package services

import com.sksamuel.elastic4s.http.search.SearchHit
import models.Business

trait ResponseMapper {
  def fromSearchHit(hit: SearchHit): Business
}
