package scala

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.ons.bi.models.BusinessIndexRec
import controllers.v1.BusinessIndexObj._
import play.api.test.FakeRequest
import play.api.test.Helpers._

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

    "check if illegal character returns 400 bad request" in {
      val name = "|"
      val search = route(app, FakeRequest(GET, s"/v1/search/BusinessName:$name")).getOrElse(sys.error("Can not find route."))
      status(search) mustBe BAD_REQUEST
    }

    "check if 500 is returned on elastic search error" in {
      val name = "!"
      val search = route(app, FakeRequest(GET, s"/v1/search/BusinessName:$name")).getOrElse(sys.error("Can not find route."))
      status(search) mustBe 500
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

    "check if exact postcode search returns only one result and is correct" in {
      val postcode = "SE13 6AS"
      go to s"$baseApiUri/v1/search/PostCode:($postcode)"
      val res = extractData(pageSource)
      ((res.length == 1) && (res(0).postCode.getOrElse(throw new Exception(s"Postcode $postcode is empty in test/sample.csv data")) == postcode)) mustBe true
    }

    "check if wildcard postcode search works correctly" in {
      val postcode = "SE13"
      go to s"$baseApiUri/v1/search/PostCode:($postcode)"
      val res = extractData(pageSource)
      res.length mustBe 2
    }

    "check that nothing is returned if Postcode does not exist" in {
      val postcode = "NNN 777"
      go to s"$baseApiUri/v1/search/PostCode:$postcode"
      pageSource mustBe "{}"
    }

    "check that search business by id works" in {
      val id = "21840175"
      go to s"$baseApiUri/v1/business/$id"
      val res = extractBusiness(pageSource)
      res.businessName mustBe "ACCLAIMED HOMES LIMITED"
    }

    "check that search business returns no results on incorrect id" in {
      val id = "0"
      go to s"$baseApiUri/v1/business/$id"
      pageSource mustBe "{}"
    }
  }

  private def extractBusiness(s: String) = Json.fromJson[BusinessIndexRec](Json.parse(s)).getOrElse(sys.error(s"error while parsing business data from elastic: $s"))

  private def extractData(s: String) = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).getOrElse(sys.error(s"error while parsing data from elastic: $s"))

  private def extractFirstData(s: String) = extractData(s).headOption.getOrElse(sys.error(s"no business data returned by elastic: $s"))
}
