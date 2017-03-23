package scala

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.ons.bi.models.BusinessIndexRec
import controllers.v1.BusinessIndexObj._

class IntegrationSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {


  "Common application" should {
    val baseApiUri = s"http://localhost:$port"

    "work from within a browser" in {
      go to baseApiUri
      pageSource must include("ONS BI DEMO")
    }
  }

  "Data Application" should {

    // wait while all data loaded into elastic
    Thread.sleep(100)

    val baseApiUri = s"http://localhost:$port"

    // following tests rely on @InsertDemoData -
    // local elastic with few loaded records
    "search for any data" in {
      def check = pageSource must include("TORUS DEVELOPMENT CONSULTANTS LIMITED")
      def request = "BusinessName:TORUS*"

      // check directly elastic
      go to s"http://localhost:9200/bi-dev/business/_search?q=$request"
      check
      // check via api
      go to s"$baseApiUri/v1/search/$request"
      check
    }

    "search with limit" in {
      val limit = 5
      go to s"$baseApiUri/v1/search/BusinessName:*?limit=$limit"
      extractData(pageSource).size mustBe limit
    }

    "check if only allowed fields" in {
      go to s"$baseApiUri/v1/search/BusinessName:*?limit=1"
      val rec = extractFirstData(pageSource)
      rec.vatRefs mustBe None
      rec.payeRefs mustBe None
      rec.employmentBands mustNot be(None)
    }

    "check if elasticsearch analyzers works" in {
      val name = "GRAFIXSTAR LTD. CORP'S & BRTHR'S"

      def checkFor(s: String) = {
        go to s"$baseApiUri/v1/search/BusinessName:$s"
        val res = extractFirstData(pageSource)
        res.businessName mustBe name
      }
      checkFor(name)
      def toSearch(in: String): String = {
        in.lastIndexOf(' ') match {
          case x if x > 0 =>
            val l = in.substring(0, x)
            checkFor(l)
            toSearch(l)
          case _ => ""
        }
      }
      toSearch(name)
    }

    "check if exact postcode search returns only one result" in {
      val postcode = "SE13 6AS"

      def checkFor(s: String): Int = {
        go to s"""$baseApiUri/v1/search/PostCode:"$s""""
        val res = extractData(pageSource)
        res.length
      }
      val t = checkFor(postcode)
      t mustBe 1
    }

    "check if exact postcode search returns correct result" in {
      val postcode = "SE13 6AS"

      def checkFor(s: String): String = {
        go to s"""$baseApiUri/v1/search/PostCode:"$s""""
        val res = extractFirstData(pageSource)
        res.postCode.get
      }
      val t = checkFor(postcode)
      t mustBe postcode
    }
  }

  private def extractData(s: String) = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).getOrElse(sys.error(s"error while parsing data from elastic: $s"))

  private def extractFirstData(s: String) = extractData(s).headOption.getOrElse(sys.error(s"no business data returned by elastic: $s"))
}
