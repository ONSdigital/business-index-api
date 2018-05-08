package scala.it

import models.Business
import org.scalatest.OptionValues
import play.api.http.HeaderNames._
import play.api.http.Status.OK

import scala.it.fixture.ServerAcceptanceSpec
import scala.sample.SampleBusiness
import scala.support.WithWireMockElasticSearch

/**
 * Created by coolit on 08/05/2018.
 */
class BusinessAcceptanceSpec extends ServerAcceptanceSpec with WithWireMockElasticSearch with OptionValues with SampleBusiness {

  private val TargetUBRN = 10205415L

  private val BusinessExactMatchESResponseBody =
    s"""{
       |"took":3,
       |"timed_out":false,
       |"_shards": {
       |  "total":5,"successful":5,"failed":0},
       |  "hits":{
       |    "total":1,
       |    "max_score":1.0,
       |    "hits":[{
       |        "_index":"bi-dev",
       |        "_type":"business",
       |        "_id":"${TargetUBRN.toString}",
       |        "_version":1,
       |        "_score":1.0,
       |        "_source":{
       |          "LegalStatus":"2",
       |          "BusinessName":"TEST GRILL LTD",
       |          "VatRefs":[105463],
       |          "IndustryCode":"86762",
       |          "CompanyNo":"29531562",
       |          "UPRN":380268,
       |          "Turnover":"A",
       |          "PayeRefs":["210926"],
       |          "TradingStatus":"A",
       |          "PostCode":"ID80 5QB",
       |          "EmploymentBands":"B"
       |        }
       |    }]
       |  }
       |}""".stripMargin

  info("As a BI user")
  info("I want to retrieve a business with a specific id")
  info("So that I can view the Business details via the user interface")

  feature("retrieve a Business") {
    scenario("by exact id (UBRN)") { wsClient =>
      Given(s"a business exists with id [$TargetUBRN]")
      stubElasticSearchFor(aBusinessIdRequest(TargetUBRN).willReturn(
        anOkResponse().withBody(BusinessExactMatchESResponseBody)
      ))

      When(s"the business with id [$TargetUBRN] is requested")
      val response = await(wsClient.url(s"/v1/business/${TargetUBRN.toString}").get())

      Then(s"the details of the unique business identified by the id [$TargetUBRN] and the business details are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe "application/json"
      response.json.as[Business] shouldBe
        Business(
          TargetUBRN, SampleBusinessName, SampleUPRN, SamplePostCode, SampleIndustryCode, SampleLegalStatus,
          SampleTradingStatus, SampleTurnover, SampleEmploymentBands, SampleVatRefs, SamplePayeRefs, SampleCompanyNo
        )
    }
  }
}
