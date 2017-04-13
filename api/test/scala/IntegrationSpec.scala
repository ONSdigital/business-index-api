package scala

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.ons.bi.models.BusinessIndexRec
import controllers.v1.BusinessIndexObj._

class IntegrationSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {


  val baseApiUri = s"http://localhost:$port"

  "Data Application" should {
    // wait while all data loaded into elastic
    "init service" in {
      go to s"$baseApiUri/v1/search/BusinessName:*"
      Thread.sleep(1000)
    }


    // following tests rely on @InsertDemoData -
    // local elastic with few loaded records
    "search for everything" in {
      go to s"$baseApiUri/v1/search/BusinessName:*"
      extractData(pageSource).size mustBe 20
    }

    "search for any data" in {
      go to s"$baseApiUri/v1/search/BusinessName:TORUS*"
      pageSource must include("TORUS DEVELOPMENT CONSULTANTS LIMITED")
    }

    "get by id" in {
      val id = 21840175L
      go to s"$baseApiUri/v1/business/$id"
      val rec = Json.fromJson[BusinessIndexRec](Json.parse(pageSource)).getOrElse(sys.error(s"Non parsed obj $pageSource"))
      rec.id mustBe id
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
      rec.companyNo mustNot be(None)
    }

    "search for special chars" in {
      go to s"$baseApiUri/v1/search/BusinessName:" + "$$$"
      val rec = extractFirstData(pageSource)
      rec.companyNo mustBe Some("77777777")
    }

    "check if industry code normalized" in {
      go to s"$baseApiUri/v1/search/IndustryCode:742?limit=1"
      val rec = extractFirstData(pageSource)
      rec.industryCode mustBe Some("00742")
    }

    "check if company no non empty" in {
      go to s"$baseApiUri/v1/search/IndustryCode:742?limit=1"
      val rec = extractFirstData(pageSource)
      rec.companyNo mustBe Some("11111111")
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
      res.length mustBe 1
      res.head.postCode.getOrElse(sys.error(s"Postcode $postcode is empty in sample data")) mustBe postcode
    }

    "check if wildcard postcode search works correctly" in {
      val postcode = "SE13"
      go to s"$baseApiUri/v1/search/PostCode:($postcode)"
      val res = extractData(pageSource)
      res.length mustBe 2
    }


    "empty results returns properly" in {
      go to s"$baseApiUri/v1/search/PostCode:UNEXISTED"
      val res = pageSource
      res must be("{}")
    }

    "invalid search must generate exception" in {
      go to s"$baseApiUri/v1/search/PostCode:^&%"
      val res = pageSource
      res must include(""""status":500""")
      res must include("query_error")
    }

    "invalid search should not generate exception" in {
      go to s"$baseApiUri/v1/search/PostCode:^&%?fail_on_bad_query=false"
      val res = pageSource
      res must include(""""status":500""")
      res must include("query_warn")
    }


  }

  private def extractData(s: String) = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).getOrElse(sys.error(s"error while parsing data from elastic: $s"))

  private def extractFirstData(s: String) = extractData(s).headOption.getOrElse(sys.error(s"no business data returned by elastic: $s"))
}
