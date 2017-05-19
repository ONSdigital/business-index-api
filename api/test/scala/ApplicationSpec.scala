package scala

import org.scalatestplus.play._
import play.api.test.Helpers.{contentAsString, _}
import play.api.test._

class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {
    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status) mustBe Some(NOT_FOUND)
    }
  }

  private[this] def fakeRequest(uri: String, method: String = GET) =
    route(app, FakeRequest(method, uri)).getOrElse(sys.error(s"Can not find route $uri."))


  "HealthController" should {
    "get the health status" in {
      val health = fakeRequest("/health")
      status(health) mustBe OK
      contentType(health) mustBe Some("text/plain")
      contentAsString(health) must include("Uptime")
      val health2 = fakeRequest("/health") // uptime value is changing
      contentAsString(health) mustNot be(contentAsString(health2))
    }
  }

  "VersionController" should {
    "get the version" in {
      val health = fakeRequest("/version")
      status(health) mustBe OK
      contentType(health) mustBe Some("application/json")
    }
  }

  "SearchController" should {
    "return 400 when no query" in {
      val search = fakeRequest("/v1/search")
      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      contentAsString(search).toLowerCase must include ("missing_query")
    }

    "return NoContent when no data" in {
      val search = fakeRequest("/v1/business/9011")
      status(search) mustBe NO_CONTENT
    }
  }
}
