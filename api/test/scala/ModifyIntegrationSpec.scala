package scala

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

/**
  * Created by Volodymyr.Glushak on 07/04/2017.
  */
class ModifyIntegrationSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {

  val baseApiUri = s"http://localhost:$port"

  "Index modification" should {

    "delete one record" in {
      go to s"$baseApiUri/v1/business/38557538"
      pageSource must include("38557538")
      go to s"$baseApiUri/v1/delete/38557538"
      pageSource mustBe "{'deleted'='true'}"
      go to s"$baseApiUri/v1/delete/38557538"
      pageSource mustBe "{'deleted'='false'}" // already removed.
    }

  }

}
