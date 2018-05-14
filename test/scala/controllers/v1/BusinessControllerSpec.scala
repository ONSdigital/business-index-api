package scala.controllers.v1

import controllers.v1.BusinessController
import models.Business
import repository.ElasticSearchBusinessRepository
import play.api.libs.json.Json
import play.api.test.FakeRequest
//import play.api.mvc.{ AnyContent, Request }
import play.api.test.Helpers.{ contentType, status, _ }
import play.mvc.Http.MimeTypes.JSON
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }

import scala.concurrent.Future
import scala.sample.SampleBusiness
import scala.concurrent.ExecutionContext.Implicits.global
import models._
import play.api.mvc.{ AnyContent, Request => Req }

class BusinessControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {
  trait Fixture extends SampleBusiness {
    val repository: ElasticSearchBusinessRepository = mock[ElasticSearchBusinessRepository]
    val TargetUbrn = 12345678L
    val TargetQuery = "BusinessName:test"
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

    //    "to retrieve a number of Businesses that match a search term" - {
    //      "returns a JSON representation of multiple businesses when found" in new Fixture {
    //        val r = mock[Req[AnyContent]]
    //        (r.getQueryString _).expects("limit").returning(Some("100"))
    //        (r.getQueryString _).expects("offset").returning(Some("0"))
    //        (r.getQueryString _).expects("default_operator").returning(Some("AND"))
    //
    //        (repository.findBusiness _).expects(TargetQuery, r).returning(
    //          Future.successful(Right(Seq(SampleBusinessWithAllFields, SampleBusinessWithAllFields1)))
    //        )
    //
    //        val request = controller.searchBusinessById(TargetUbrn)
    //        val response = request.apply(FakeRequest())
    //        1 shouldBe 1
    //
    //        //        status(response) shouldBe OK
    //        //        contentType(response) shouldBe Some(JSON)
    //        //        contentAsJson(response) shouldBe Json.toJson(TargetBusiness)
    //      }
    //    }

    "containing invalid arguments" - {
      "receives a BAD_REQUEST status response" in new Fixture {
        val request = controller.badRequest(TargetUbrn.toString)
        val response = request.apply(FakeRequest())

        status(response) shouldBe BAD_REQUEST
      }
    }
  }
}
