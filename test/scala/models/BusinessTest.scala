package scala.models

import models.Business
import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.Json

import support.JsonString
import support.JsonString._

/**
 * Created by coolit on 04/05/2018.
 */
class BusinessTest extends FreeSpec with Matchers {

  private trait Fixture {
    def expectedJsonStrOf(business: Business): String =
      JsonString.withObject(
        long("id", business.id),
        string("businessName", business.businessName),
        optionalLong("uprn", business.uprn),
        optionalString("postCode", business.postCode),
        optionalString("industryCode", business.industryCode),
        optionalString("legalStatus", business.legalStatus),
        optionalString("tradingStatus", business.tradingStatus),
        optionalString("turnover", business.turnover),
        optionalString("employmentBands", business.employmentBands),
        optionalSeqString("vatRefs", business.vatRefs),
        optionalSeqString("vatRefs", business.payeRefs),
        optionalString("companyNo", business.companyNo)
      )
  }

  "A Business" - {
    "can be represented as JSON" - {
      "when all fields are defined" ignore new Fixture {
        val business = Business(12345, "Big Company", Some(100L), Some("NP20 ABC"), Some("A"), Some("1"), Some("2"),
          Some("3"), Some("4"), Some(Seq("1123123,23324234")), Some(Seq("3424,23434")), Some("8797984"))

        Json.toJson(business) shouldBe Json.parse(expectedJsonStrOf(business))
      }

      "when only mandatory fields are defined" ignore new Fixture {
        val business = Business(12345, "Big Company", None, None, None, None, None,
          None, None, None, None, None)

        Json.toJson(business) shouldBe Json.parse(expectedJsonStrOf(business))
      }
    }
  }
}
