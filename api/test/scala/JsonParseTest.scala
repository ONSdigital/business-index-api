package scala

import controllers.v1.BusinessIndexObj.businessHitFormat
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
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
    """{"id":85282745,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":42751,"legalStatus":"1"}"""

  "It" should "create proper json" in {
    parseJson(fullJson).map(_.id).getOrElse(0L) shouldBe 85282744
    parseJson(smallerJson).map(_.id).getOrElse(0L) shouldBe 85282745

    Json.toJson(parseJson(smallerJson).getOrElse(sys.error("Can't parse"))).toString() shouldBe smallerJson
  }

  private[this] def parseJson(x: String) = Json.fromJson[BusinessIndexRec](Json.parse(x))

  "It" should "create json from obj" in {
    val data = BusinessIndexRec(16332123, "B.M.J. HOMES CO. LTD", Some(316786), Some("RJ87 4WK"),
      Some(19946), Some("6"), Some("D"), Some("F"), Some("L"), Some(Seq(12076)), Some(Seq("24152")), Some("AB123456"))
    Json.toJson(data)
  }

}
