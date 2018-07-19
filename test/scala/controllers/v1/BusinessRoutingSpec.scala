package scala.controllers.v1

import org.scalatest.{ FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.controllers.v1.fixture.HttpServerErrorStatusCode

class BusinessRoutingSpec extends FreeSpec with GuiceOneAppPerSuite with Matchers {
  override def fakeApplication() = new GuiceApplicationBuilder().configure(Map("db.hbase-rest.timeout" -> "100")).build()

  private trait Fixture extends HttpServerErrorStatusCode {
    val ValidUbrnShort = "12345678"
    val ValidUbrnLong = "1234567890123456"

    val fakeRequest: (String) => Option[Future[Result]] = (url: String) => route(app, FakeRequest(GET, url))
  }

  "A request to retrieve a Business by a Unique Business Reference Number (UBRN)" - {
    "is rejected when" - {
      "the UBRN" - {
        "has fewer than 8 digits" in new Fixture {
          val UbrnWithFewerDigits = ValidUbrnShort.drop(1)
          val Some(result) = fakeRequest(s"/v1/business/$UbrnWithFewerDigits")

          status(result) shouldBe BAD_REQUEST
        }

        "has more than 16 digits" in new Fixture {
          val UbrnWithMoreDigits = ValidUbrnLong + "1"
          val Some(result) = fakeRequest(s"/v1/business/$UbrnWithMoreDigits")

          status(result) shouldBe BAD_REQUEST
        }

        "is a non numerical value" in new Fixture {
          val UbrnWithNonNumericalValue = new String(Array.fill(ValidUbrnShort.length)('A'))
          val Some(result) = fakeRequest(s"/v1/business/$UbrnWithNonNumericalValue")

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
