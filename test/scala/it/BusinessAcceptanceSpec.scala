package scala.it

import models.Business
import org.scalatest.OptionValues
import play.api.http.HeaderNames._
import play.api.http.Status.OK

import scala.it.fixture.ServerAcceptanceSpec
import scala.sample.{ SampleBusiness, SampleBusinessJson }
import scala.support.WithWireMockElasticSearch

/**
 * Created by coolit on 08/05/2018.
 */
class BusinessAcceptanceSpec extends ServerAcceptanceSpec with WithWireMockElasticSearch with OptionValues
    with SampleBusiness with SampleBusinessJson {

  private val TargetUBRN = 10205415L
  private val BusinessSearch = "test%20g*"

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
      response.json.as[List[Business]].length shouldBe 2
      response.json.as[List[Business]].head shouldBe SampleBusinessWithAllFields.secured
      response.json.as[List[Business]].tail.head shouldBe SampleBusinessWithAllFields1.secured
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
      response.json.as[List[Business]].length shouldBe 2
      response.json.as[List[Business]].head shouldBe SampleBusinessWithAllFields.secured
      response.json.as[List[Business]].tail.head shouldBe SampleBusinessWithAllFields1.secured
    }
  }
}
