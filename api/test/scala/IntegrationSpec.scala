package scala

import org.scalatestplus.play._

class IntegrationSpec extends PlaySpec with OneServerPerTest with OneBrowserPerTest with HtmlUnitFactory {
  "Application" should {
    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include("ONS BI DEMO")
    }
  }
}
