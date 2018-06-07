package scala.controllers.v1

import controllers.v1.BusinessController
import models.Business
import repository.ElasticSearchBusinessRepository
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{ contentType, status, _ }
import play.mvc.Http.MimeTypes.JSON
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ FreeSpec, Matchers, OptionValues }

import scala.concurrent.Future
import scala.sample.SampleBusiness
import models._

class BusinessControllerSpec extends FreeSpec with Matchers with MockFactory with OptionValues {

  trait Fixture extends SampleBusiness {
    val repository: ElasticSearchBusinessRepository = mock[ElasticSearchBusinessRepository]
    val TargetUbrn = 12345678L
    val TargetQuery = "query=BusinessName:test"
    val TargetEmptyQuery = ""
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
      "returns a JSON representation of multiple businesses when found" in new Fixture {
        val query = BusinessSearchRequest(TargetQuery, 0, 10000, "AND")
        val businesses = Seq(SampleBusinessWithAllFields.secured, SampleBusinessWithAllFields1.secured)

        (repository.findBusiness _).expects(query).returning(
          Future.successful(Right(FindBusinessResult(businesses, businesses.length)))
        )

        val request = controller.searchBusiness(Some(TargetQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe OK
        contentType(response) shouldBe Some(JSON)
        contentAsJson(response).as[Seq[Business]] should contain theSameElementsInOrderAs
          Seq(SampleBusinessWithAllFields.secured, SampleBusinessWithAllFields1.secured)
      }

      "returns NOT_FOUND when a business with a valid UBRN is not found" in new Fixture {
        val query = BusinessSearchRequest(TargetQuery, 0, 10000, "AND")

        (repository.findBusiness _).expects(query).returning(
          Future.successful(Right(FindBusinessResult(Seq(), 0)))
        )

        val request = controller.searchBusiness(Some(TargetQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe NOT_FOUND
      }

      "returns GATEWAY_TIMEOUT when a retrieval time exceeds the configured time out" in new Fixture {
        val query = BusinessSearchRequest(TargetQuery, 0, 10000, "AND")

        (repository.findBusiness _).expects(query).returning(
          Future.successful(Left(GatewayTimeout("Gateway Timeout.")))
        )

        val request = controller.searchBusiness(Some(TargetQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe GATEWAY_TIMEOUT
      }

      "returns INTERNAL_SERVER_ERROR when an error occurs during the retrieval of Businesses" in new Fixture {
        val query = BusinessSearchRequest(TargetQuery, 0, 10000, "AND")

        (repository.findBusiness _).expects(query).returning(
          Future.successful(Left(InternalServerError("Internal Server Error.")))
        )

        val request = controller.searchBusiness(Some(TargetQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "containing invalid arguments" - {
      "returns BAD_REQUEST if there is no query present" in new Fixture {
        val request = controller.searchBusiness(Some(TargetEmptyQuery))
        val response = request.apply(FakeRequest())

        status(response) shouldBe BAD_REQUEST
      }

      // This will test that the query is trimmed correctly
      "returns BAD_REQUEST if there a query is empty" in new Fixture {
        val request = controller.searchBusiness(Some(TargetEmptyQuery + " "))
        val response = request.apply(FakeRequest())

        status(response) shouldBe BAD_REQUEST
      }

      "receives a BAD_REQUEST status response" in new Fixture {
        val request = controller.badRequest(TargetUbrn.toString)
        val response = request.apply(FakeRequest())

        status(response) shouldBe BAD_REQUEST
      }
    }
  }
}
