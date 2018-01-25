package scala

import org.scalatest.{ FlatSpec, Matchers }
import play.api.libs.json._
import uk.gov.ons.bi.models.BusinessIndexRec

/**
 * Created by Volodymyr.Glushak on 01/03/2017.
 */
class JsonParseTest extends FlatSpec with Matchers {

  private[this] val fullJson =
    """
      |{"id":85282744,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":null,"legalStatus":"1","tradingStatus":"A","turnover":"B","employmentBands":"E"}
    """.stripMargin

  private[this] val smallerJson =
    """{"id":85282745,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":"42751","legalStatus":"1"}"""

  private[this] val smallerBiRec = BusinessIndexRec(85282745, "BI (2018) LIMITED", Some(977146940701L), Some("SE"), Some("42751"), Some("1"),
    None, None, None, None, None, None)

  private[this] val smallerExtendedJson =
    """{"id":85282745,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":"42751","legalStatus":"1","tradingStatus":"","turnover":"","employmentBands":"","companyNo":""}"""

  "It" should "create proper json" in {
    Json.fromJson[BusinessIndexRec](Json.parse(fullJson)).get.id shouldBe 85282744
    Json.fromJson[BusinessIndexRec](Json.parse(smallerJson)).get.id shouldBe 85282745

    val bi: BusinessIndexRec = Json.fromJson[BusinessIndexRec](Json.parse(smallerJson)).get
    Json.toJson(bi).toString() shouldBe smallerExtendedJson
  }

  "It" should "create json from obj" in {
    val data = BusinessIndexRec(16332123, "B.M.J. HOMES CO. LTD", Some(316786), Some("RJ87 4WK"),
      Some("19946"), Some("6"), Some("D"), Some("F"), Some("L"), Some(Seq(12076)), Some(Seq("24152")), Some("AB123456"))
    Json.toJson(data)
  }

  "It" should "produce some empty values in smaller json" in {
    Json.toJson(smallerBiRec).toString() shouldBe smallerExtendedJson
  }

}
