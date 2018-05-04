package scala

import play.api.test.Helpers._
import play.api.test._

import support.TestUtils

/**
 * Test application routes operate
 */
class RouteSpec extends TestUtils {

  "No Route" should {
    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status) mustBe Some(NOT_FOUND)
    }
  }

  "HomeController" should {
    "render default app route" in {
      val home = fakeRequest("/")
      status(home) mustEqual OK
    }

    "display swagger documentation" in {
      val docs = fakeRequest("/docs")
      status(docs) mustEqual SEE_OTHER
      val res = getValue(redirectLocation(docs))
      res must include("/swagger-ui/index.html")
      contentAsString(docs) mustNot include("Not_FOUND")
    }
  }

  "BusinessController" should {
    "return 400 short key length when searching for incorrect id" in {
      val search = fakeRequest("/v1/business/1")
      status(search) mustBe BAD_REQUEST
    }

    "return 400 key length too long when searching for incorrect id" in {
      val dateSearch = fakeRequest("/v1/business/12345678901234567892")
      status(dateSearch) mustBe BAD_REQUEST
    }
  }

  "VersionController" should {
    "display application name and version" in {
      val version = fakeRequest("/version", GET)
      status(version) mustBe OK
      contentType(version) mustBe Some("application/json")
      (contentAsJson(version) \ "name").as[String] mustBe "ons-business-index-api"
      (contentAsJson(version) \ "version").asOpt[String].isEmpty mustBe false
    }
  }
}
