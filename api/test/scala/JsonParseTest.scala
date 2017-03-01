package scala

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json
import uk.gov.ons.bi.models.BusinessIndexRec

/**
  * Created by Volodymyr.Glushak on 01/03/2017.
  */
class JsonParseTest extends FlatSpec with Matchers {

  import controllers.v1.BusinessIndexObj._

  private[this] val fullJson =
    """
      |{"id":85282744,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":null,"legalStatus":"1","tradingStatus":"A","turnover":"B","employmentBands":"E"}
    """.stripMargin

  private[this] val smallerJson =
    """{"id":85282745,"businessName":"BI (2018) LIMITED","uprn":977146940701,"postCode":"SE","industryCode":42751,"legalStatus":"1"}"""

  "It" should "create proper json" in {
    parseJson(fullJson).get.id shouldBe 85282744
    val bi = parseJson(smallerJson).get
    bi.id shouldBe 85282745

    Json.toJson(bi).toString() shouldBe smallerJson
  }

  private[this] def parseJson(x: String) = Json.fromJson[BusinessIndexRec](Json.parse(x))

}
