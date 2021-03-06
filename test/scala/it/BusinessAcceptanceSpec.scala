package scala.it

import models.Business
import org.scalatest.OptionValues
import play.api.http.HeaderNames._
import play.api.http.Status.{ OK, NOT_FOUND, BAD_REQUEST }

import scala.it.fixture.ServerAcceptanceSpec
import scala.sample.{ SampleBusiness, SampleBusinessJson }
import scala.support.WithWireMockElasticSearch

class BusinessAcceptanceSpec extends ServerAcceptanceSpec with WithWireMockElasticSearch with OptionValues
    with SampleBusiness with SampleBusinessJson {

  private val TargetUBRN = 10205415L
  private val InvalidTargetUBRN = 12L
  private val BusinessSearch = "test%20g*" // The '%20' is to represent an encoded space
  private val InvalidBusinessSearch = ""

  info("As a BI user")
  info("I want to retrieve a business")
  info("So that I can view the Business details via the user interface")

  feature("retrieve a Business") {
    scenario("by exact id (UBRN)") { wsClient =>
      Given(s"a business exists with id [$TargetUBRN]")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessExactMatchESResponseBody)
      ))

      When(s"the business with id [$TargetUBRN] is requested")
      val response = await(wsClient.url(s"/v1/business/${TargetUBRN.toString}").get())

      Then(s"the details of the unique business identified by the id [$TargetUBRN] and the business details are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[Business] shouldBe SampleBusinessWithAllFields
    }

    scenario("by searching using a fuzzy match") { wsClient =>
      info("the fuzzy match search will not return values for the fields: vatRefs, payeRefs or UPRN")
      Given(s"businesses exists with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessFuzzyMatchESResponseBody)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search?query=BusinessName:$BusinessSearch").get())

      Then(s"the details of the businesses matching the search term [$BusinessSearch] are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[Seq[Business]].length shouldBe 2
      response.json.as[Seq[Business]] should contain theSameElementsInOrderAs
        Seq(SampleBusinessWithAllFields.secured, SampleBusinessWithAllFields1.secured)
      response.header("X-Total-Count") shouldBe Some("2")
    }

    scenario("by searching using a fuzzy match using a search term") { wsClient =>
      info("the search term means using ?query= is redundant")
      info("the fuzzy match search will not return values for the fields: vatRefs, payeRefs or UPRN")
      Given(s"businesses exists with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessFuzzyMatchESResponseBody)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search/BusinessName:$BusinessSearch").get())

      Then(s"the details of the businesses matching the search term [$BusinessSearch] are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[Seq[Business]].length shouldBe 2
      response.json.as[Seq[Business]] should contain theSameElementsInOrderAs
        Seq(SampleBusinessWithAllFields.secured, SampleBusinessWithAllFields1.secured)
      response.header("X-Total-Count") shouldBe Some("2")
    }
  }

  feature("retrieve a Business (with only required fields present in ElasticSearch)") {
    scenario("by exact id (UBRN)") { wsClient =>
      Given(s"a business exists with id [$TargetUBRN]")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessExactMatchESResponseBodyEmptyFields)
      ))

      When(s"the business with id [$TargetUBRN] is requested")
      val response = await(wsClient.url(s"/v1/business/${TargetUBRN.toString}").get())

      Then(s"the details of the unique business identified by the id [$TargetUBRN] and the business details are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[Business] shouldBe SampleBusinessWithNoOptionalFields
    }

    scenario("by searching using a fuzzy match") { wsClient =>
      info("the fuzzy match search will not return values for the fields: vatRefs, payeRefs or UPRN")
      Given(s"businesses exists with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessFuzzyMatchESResponseBodyEmptyFields)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search?query=BusinessName:$BusinessSearch").get())

      Then(s"the details of the businesses matching the search term [$BusinessSearch] are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[List[Business]].length shouldBe 2
      response.json.as[List[Business]].head shouldBe SampleBusinessWithNoOptionalFields.secured
      response.json.as[List[Business]].tail.head shouldBe SampleBusinessWithNoOptionalFields1.secured
      response.header("X-Total-Count") shouldBe Some("2")
    }

    scenario("by searching using a fuzzy match using a search term") { wsClient =>
      info("the search term means using ?query= is redundant")
      info("the fuzzy match search will not return values for the fields: vatRefs, payeRefs or UPRN")
      Given(s"businesses exists with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(BusinessFuzzyMatchESResponseBodyEmptyFields)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search/BusinessName:$BusinessSearch").get())

      Then(s"the details of the businesses matching the search term [$BusinessSearch] are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[List[Business]].length shouldBe 2
      response.json.as[List[Business]].head shouldBe SampleBusinessWithNoOptionalFields.secured
      response.json.as[List[Business]].tail.head shouldBe SampleBusinessWithNoOptionalFields1.secured
      response.header("X-Total-Count") shouldBe Some("2")
    }
  }

  feature("retrieve a non-existent Business") {
    scenario("by exact id (UBRN)") { wsClient =>
      Given(s"no business exists with id [$TargetUBRN]")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(NotFound)
      ))

      When(s"the business with id [$TargetUBRN] is requested")
      val response = await(wsClient.url(s"/v1/business/${TargetUBRN.toString}").get())

      Then(s"as a business does not exist for [$TargetUBRN], 404 NOT FOUND is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("by searching using a fuzzy match") { wsClient =>
      Given(s"no businesses exist with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(NotFound)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search?query=BusinessName:$BusinessSearch").get())

      Then(s"as no businesses match the search term [$BusinessSearch], 404 NOT FOUND is returned")
      response.status shouldBe NOT_FOUND
    }

    scenario("by searching using a fuzzy match using a search term") { wsClient =>
      Given(s"no businesses exists with a business name that matches the following: 'test g*'")
      stubElasticSearchFor(aBusinessQuery().willReturn(
        anOkResponse().withBody(NotFound)
      ))

      When(s"the businesses matching the this term [$BusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search/BusinessName:$BusinessSearch").get())

      Then(s"no businesses match the search term [$BusinessSearch], so 404 NOT FOUND is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("validate request parameters") {
    scenario("by exact id (UBRN)") { wsClient =>
      Given(s"the id [$InvalidTargetUBRN] is not a valid UBRN")

      When(s"the business with id [$TargetUBRN] is requested")
      val response = await(wsClient.url(s"/v1/business/${InvalidTargetUBRN.toString}").get())

      Then(s"as the id [$TargetUBRN] is invalid, 400 BAD REQUEST is returned")
      response.status shouldBe BAD_REQUEST
    }

    scenario("by searching using a fuzzy match") { wsClient =>
      Given(s"no search term is provided")

      When(s"the businesses matching the this term [$InvalidBusinessSearch] are requested")
      val response = await(wsClient.url(s"/v1/search?query=$InvalidBusinessSearch").get())

      Then(s"as the search term [$InvalidBusinessSearch] is invalid, 400 BAD REQUEST is returned")
      response.status shouldBe BAD_REQUEST
    }
  }
}
