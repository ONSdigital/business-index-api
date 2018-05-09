package scala.utils

import com.sksamuel.elastic4s.http.search.{ SearchHit, SearchHits, SearchResponse }
import com.sksamuel.elastic4s.http.{ RequestSuccess, Shards }
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers }
import utils.ElasticRequestMapper

import scala.sample.SampleBusiness

class ElasticRequestMapperTest extends FreeSpec with Matchers with MockFactory with SampleBusiness {

  private trait Fixture {
    val elasticRequestMapper = new ElasticRequestMapper()
  }

  private def createRequestSuccess(hitsArr: Array[SearchHit]): RequestSuccess[SearchResponse] = {
    val shards = Shards(2, 0, 2)
    val hits = SearchHits(100L, 20.0, hitsArr)
    val searchResp = SearchResponse(100L, false, false, Map(), shards, None, Map(), hits)
    RequestSuccess[SearchResponse](200, None, Map(), searchResp)
  }

  "The ElasticRequestMapper" - {
    "can convert a RequestSuccess to a None if there are no results (Business)" in new Fixture {
      val requestSuccess = createRequestSuccess(Array())
      elasticRequestMapper.fromBusinessResponse(requestSuccess) shouldBe None
    }

    "can convert a RequestSuccess to a Seq() if there are no results (Seq[Business])" in new Fixture {
      val requestSuccess = createRequestSuccess(Array())
      elasticRequestMapper.fromBusinessSeqResponse(requestSuccess) shouldBe Seq()
    }

    "can convert a RequestSuccess to a Business if there are search results" in new Fixture {
      val searchHit = SearchHit(
        SampleBusinessId.toString, "", "", 1L, 1F, None, None, None, None, None,
        Map("BusinessName" -> SampleBusinessName), Map(), Map(), Map()
      )
      val requestSuccess = createRequestSuccess(Array(searchHit))
      elasticRequestMapper.fromBusinessResponse(requestSuccess) shouldBe Some(SampleBusinessWithNoOptionalFields)
    }

    "can convert a RequestSuccess to a Seq[Business] if there are multiple results" in new Fixture {
      val searchHit = SearchHit(
        SampleBusinessId.toString, "", "", 1L, 1F, None, None, None, None, None,
        Map("BusinessName" -> SampleBusinessName), Map(), Map(), Map()
      )

      val searchHit1 = SearchHit(
        SampleBusinessId1.toString, "", "", 1L, 1F, None, None, None, None, None,
        Map("BusinessName" -> SampleBusinessName1), Map(), Map(), Map()
      )

      val requestSuccess = createRequestSuccess(Array(searchHit, searchHit1))
      elasticRequestMapper.fromBusinessSeqResponse(requestSuccess) shouldBe Seq(SampleBusinessWithNoOptionalFields, SampleBusinessWithNoOptionalFields1)
    }

    "can convert a RequestSuccess to a Business even if required fields are missing" in new Fixture {
      val searchHit = SearchHit(
        SampleBusinessId.toString, "", "", 1L, 1F, None, None, None, None, None, Map(), Map(), Map(), Map()
      )
      val requestSuccess = createRequestSuccess(Array(searchHit))

      val maybeBusiness = elasticRequestMapper.fromBusinessResponse(requestSuccess)
      maybeBusiness shouldBe Some(SampleBusinessWithNoOptionalFields.copy(businessName = ""))
    }
  }
}
