package scala.controllers.v1

import controllers.v1.BusinessController
import models.Business
import repository.ElasticSearchBusinessRepository
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentType, status, _ }
import play.mvc.Http.MimeTypes.JSON
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues, PrivateMethodTester }

import scala.concurrent.Future
import scala.sample.SampleBusiness
import models._
import play.api.mvc.{ AnyContent, Result, Request => Req }

class BusinessControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues with PrivateMethodTester {
  trait Fixture extends SampleBusiness {
    val repository: ElasticSearchBusinessRepository = mock[ElasticSearchBusinessRepository]
    val TargetUbrn = 12345678L
    val TargetQuery = "query=BusinessName:test"
    val TargetBusiness: Business = aBusinessSample(TargetUbrn)

    val controller = new BusinessController(repository)
  }

  "A request" - {
    "to retrieve a Business by it's Unique Business Reference Number (UBRN)" - {
      "returns a JSON representation of a Business when found" in new Fixture {
        (repository.findBusinessById _).expects(TargetUbrn).returning(
          Future.successful(Right(Some(TargetBusiness)))
        )

        val request = controller.searchBusinessById(TargetUbrn)
        val response = request.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response) shouldBe Some(JSON)
        contentAsJson(response) shouldBe Json.toJson(TargetBusiness)
      }

      "returns NOT_FOUND when a business with a valid UBRN is not found" in new Fixture {
        (repository.findBusinessById _).expects(TargetUbrn).returning(
          Future.successful(Right(None))
        )

        val request = controller.searchBusinessById(TargetUbrn)
        val response = request.apply(FakeRequest())

        status(response) shouldBe NOT_FOUND
      }

      "returns GATEWAY_TIMEOUT when a retrieval time exceeds the configured time out" in new Fixture {
        (repository.findBusinessById _).expects(TargetUbrn).returning(
          Future.successful(Left(GatewayTimeout("Timeout.")))
        )

        val request = controller.searchBusinessById(TargetUbrn)
        val response = request.apply(FakeRequest())

        status(response) shouldBe GATEWAY_TIMEOUT
      }

      "returns INTERNAL_SERVER_ERROR when an error occurs during the retrieval of a Business" in new Fixture {
        (repository.findBusinessById _).expects(TargetUbrn).returning(
          Future.successful(Left(InternalServerError("Internal Server Error.")))
        )

        val request = controller.searchBusinessById(TargetUbrn)
        val response = request.apply(FakeRequest())

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "to retrieve a number of Businesses that match a search term" - {
      "returns a JSON representation of multiple businesses when found" ignore new Fixture {
        val r = mock[Req[AnyContent]]
        (r.getQueryString _).expects("limit").returning(None)
        (r.getQueryString _).expects("offset").returning(None)
        (r.getQueryString _).expects("default_operator").returning(None)
        //        (r.getQueryString _).expects("limit").returning(Some("100"))
        //        (r.getQueryString _).expects("offset").returning(Some("0"))
        //        (r.getQueryString _).expects("default_operator").returning(Some("AND"))

        (repository.findBusiness _).expects(TargetQuery, r).returning(
          Future.successful(Right(Seq(SampleBusinessWithAllFields, SampleBusinessWithAllFields1)))
        )

        val request = controller.searchBusiness(Some(TargetQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response) shouldBe Some(JSON)
        contentAsJson(response) shouldBe Json.toJson(TargetBusiness)
      }
    }

    "containing invalid arguments" - {
      "receives a BAD_REQUEST status response" in new Fixture {
        val request = controller.badRequest(TargetUbrn.toString)
        val response = request.apply(FakeRequest())

        status(response) shouldBe BAD_REQUEST
      }
    }
  }

  "BusinessController private methods" - {
    "translates an ErrorMessage into the correct corresponding response type" - {
      "InternalServerError" in new Fixture {
        val resultOnFailure = PrivateMethod[Result]('resultOnFailure)
        val response = controller invokePrivate resultOnFailure(InternalServerError("Internal Server Error"))
        response.header.status shouldBe INTERNAL_SERVER_ERROR
      }

      "GatewayTimeout" in new Fixture {
        val resultOnFailure = PrivateMethod[Result]('resultOnFailure)
        val response = controller invokePrivate resultOnFailure(GatewayTimeout("Gateway Timeout."))
        response.header.status shouldBe GATEWAY_TIMEOUT
      }

      "ServiceUnavailable" in new Fixture {
        val resultOnFailure = PrivateMethod[Result]('resultOnFailure)
        val response = controller invokePrivate resultOnFailure(ServiceUnavailable("Service Unavailable."))
        response.header.status shouldBe SERVICE_UNAVAILABLE
      }
    }

    "translates a successful result" - {
      "from a Some(Business) to an OK" in new Fixture {
        val resultOnSuccess = PrivateMethod[Result]('resultOnSuccess)
        val response = controller invokePrivate resultOnSuccess(Some(SampleBusinessWithAllFields))
        response.header.status shouldBe OK
      }

      "from a None to a NOT_FOUND" in new Fixture {
        val resultOnSuccess = PrivateMethod[Result]('resultOnSuccess)
        val response = controller invokePrivate resultOnSuccess(None)
        response.header.status shouldBe NOT_FOUND
      }

      "from a non empty Seq[Business] to an OK" in new Fixture {
        val resultSeqOnSuccess = PrivateMethod[Result]('resultSeqOnSuccess)
        val response = controller invokePrivate resultSeqOnSuccess(Seq(SampleBusinessWithAllFields, SampleBusinessWithAllFields1))
        response.header.status shouldBe OK
      }

      "from an empty Seq[Business] to a NOT_FOUND" in new Fixture {
        val resultSeqOnSuccess = PrivateMethod[Result]('resultSeqOnSuccess)
        val response = controller invokePrivate resultSeqOnSuccess(Seq())
        response.header.status shouldBe NOT_FOUND
      }
    }
  }
}
