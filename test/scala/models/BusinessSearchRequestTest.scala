package scala.models

import models.BusinessSearchRequest
import org.scalatest.{ FreeSpec, Matchers }
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{ AnyContent, Request }

class BusinessSearchRequestSpec extends FreeSpec with Matchers with MockFactory {
  "A BusinessSearchRequest" - {
    "can be created given a search term and a request" in {
      val request = mock[Request[AnyContent]]
      (request.getQueryString _).expects("limit").returning(Some("100"))
      (request.getQueryString _).expects("offset").returning(Some("0"))
      (request.getQueryString _).expects("default_operator").returning(Some("AND"))

      val searchRequest = BusinessSearchRequest("BusinessName:test", request)
      searchRequest shouldBe BusinessSearchRequest("BusinessName:test", 0, 100, false, "AND")
    }
  }

  "can be created given a search term and an incomplete request" in {
    val request = mock[Request[AnyContent]]
    (request.getQueryString _).expects("limit").returning(None)
    (request.getQueryString _).expects("offset").returning(None)
    (request.getQueryString _).expects("default_operator").returning(None)

    val searchRequest = BusinessSearchRequest("BusinessName:test", request)
    searchRequest shouldBe BusinessSearchRequest("BusinessName:test", 0, 10000, false, "AND")
  }
}
