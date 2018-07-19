package utils

import com.sksamuel.elastic4s.http.search.SearchHit
import models.Business
import services.ResponseMapper

/**
 * This class uses .secured to ensure that the VatRefs/PayeRefs/UPRN values of the Business model are None. A seperate
 * class is used to ensure that the secured functionality is immediately obvious.
 */
class ElasticResponseMapperSecured extends ResponseMapper {
  def fromSearchHit(hit: SearchHit): Business = Business.fromMap(hit.id.toLong, hit.sourceAsMap).secured
}
