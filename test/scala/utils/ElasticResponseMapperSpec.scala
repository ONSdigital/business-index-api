package scala.utils

import com.sksamuel.elastic4s.http.search.SearchHit
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import utils.ElasticResponseMapper

import scala.sample.SampleBusiness

class ElasticResponseMapperSpec extends FreeSpec with Matchers with MockFactory with SampleBusiness {

  private trait Fixture {
    val elasticResponseMapper = new ElasticResponseMapper()
  }

  "The ElasticResponseMapper" - {
    "can convert a SearchHit to a Business" in new Fixture {
      val searchHit = SearchHit(
        SampleBusinessId.toString, "", "", 1L, 1F, None, None, None, None, None,
        Map("BusinessName" -> SampleBusinessName), Map(), Map(), Map()
      )
      elasticResponseMapper.fromSearchHit(searchHit) shouldBe SampleBusinessWithNoOptionalFields
    }
  }
}
