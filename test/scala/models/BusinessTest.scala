package scala.models

import models.Business
import org.scalatest.{ FreeSpec, Matchers }
import play.api.libs.json.Json

import scala.sample.SampleBusiness
import support.JsonString
import support.JsonString._

class BusinessSpec extends FreeSpec with Matchers with SampleBusiness {

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
        optionalSeqString("payeRefs", business.payeRefs),
        optionalString("companyNo", business.companyNo)
      )
  }

  "A Business" - {
    "can be represented as JSON" - {
      "when all fields are defined" ignore new Fixture {
        Json.toJson(SampleBusinessWithAllFields) shouldBe Json.parse(expectedJsonStrOf(SampleBusinessWithAllFields))
      }

      "when only mandatory fields are defined" ignore new Fixture {
        Json.toJson(SampleBusinessWithNoOptionalFields) shouldBe Json.parse(expectedJsonStrOf(SampleBusinessWithNoOptionalFields))
      }
    }

    "can be represented securely with no UPRN or vat/paye refs" - {
      "when all fields are defined" in new Fixture {
        val business = Business(12345, "Big Company", Some(100L), Some("NP20 ABC"), Some("A"), Some("1"), Some("2"),
          Some("3"), Some("4"), Some(Seq("1123123,23324234")), Some(Seq("3424,23434")), Some("8797984"))

        val businessSecured = Business(12345, "Big Company", None, Some("NP20 ABC"), Some("A"), Some("1"), Some("2"),
          Some("3"), Some("4"), None, None, Some("8797984"))

        business.secured shouldBe businessSecured
      }
    }
  }
}
