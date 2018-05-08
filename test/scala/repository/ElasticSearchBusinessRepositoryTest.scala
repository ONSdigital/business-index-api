package scala.repository

import models.BusinessSearchRequest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}
import play.api.mvc.{AnyContent, Request}

/**
  * Created by coolit on 04/05/2018.
  */
class ElasticSearchBusinessRepositoryTest extends FreeSpec with Matchers with MockFactory {
  "A BusinessSearchRequest" - {
    "can be created given a search term and a request" in {
      val request = mock[Request[AnyContent]]
      (request.getQueryString _).expects("limit").returning(
        Some("100")
      )

      (request.getQueryString _).expects("offset").returning(
        Some("0")
      )

      (request.getQueryString _).expects("default_operator").returning(
        Some("AND")
      )

      val searchRequest = BusinessSearchRequest("BusinessName:test", request)
      searchRequest shouldBe BusinessSearchRequest("BusinessName:test", 0, 100, false, "AND")
    }
  }
}
