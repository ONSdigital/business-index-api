package scala

import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._

class ApplicationSpec extends PlaySpec with OneAppPerTest {

  "Routes" should {
    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status) mustBe Some(NOT_FOUND)
    }
  }

  "HomeController" should {
    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).getOrElse(sys.error("Can not find route."))

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
    }
  }

  "SearchController" should {
    "return 400 when no query" in {
      val search = route(app, FakeRequest(GET, "/v1/search")).getOrElse(sys.error("Can not find route."))

      status(search) mustBe BAD_REQUEST
      contentType(search) mustBe Some("application/json")
      contentAsString(search).toLowerCase must include ("missing_query")
    }
  }

  "VersionController" should {
    "return version information as JSON" in {
      val version = route(app, FakeRequest(GET, "/version")).getOrElse(sys.error("Cannot find route."))

      status(version) mustBe OK
      contentType(version) mustBe Some("application/json")
      contentAsString(version) must include ("ons-bi-api")
    }
  }

  "HealthController" should {
    "return health information as JSON" in {
      val health = route(app, FakeRequest(GET, "/health")).getOrElse(sys.error("Cannot find route."))

      status(health) mustBe OK
      contentType(health) mustBe Some("application/json")
      contentAsString(health) must include ("Date and Time:")
    }
  }
}
