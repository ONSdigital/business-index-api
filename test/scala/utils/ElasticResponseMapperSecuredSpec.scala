package scala.utils

import com.sksamuel.elastic4s.http.search.SearchHit
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import utils.ElasticResponseMapperSecured

import scala.sample.SampleBusiness

class ElasticResponseMapperSecuredSpec extends FreeSpec with Matchers with MockFactory with SampleBusiness {

  private trait Fixture {
    val elasticResponseMapperSecured = new ElasticResponseMapperSecured()
  }

  "The ElasticResponseMapper" - {
    "can convert a SearchHit to a Business" in new Fixture {
      val searchHit = SearchHit(
        SampleBusinessId.toString, "", "", 1L, 1F, None, None, None, None, None,
        Map("BusinessName" -> SampleBusinessName), Map(), Map(), Map()
      )
      elasticResponseMapperSecured.fromSearchHit(searchHit) shouldBe SampleBusinessWithNoOptionalFields.secured
    }
  }
}
