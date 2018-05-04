//package scala
//
//import org.scalatestplus.play._
//import org.scalatestplus.play.guice.GuiceOneServerPerSuite
//import play.api.libs.json._
//import uk.gov.ons.bi.models.BusinessIndexRec
//
//class IntegrationISpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {
//
//  val mockUri = s"http://localhost:$port"
//
//  val realUri: Option[String] = sys.props.get("test.server")
//  val integrationMode: Option[String] = sys.props.get("integration.test")
//  if (integrationMode.exists(_.toBoolean) && realUri.isEmpty) sys.error("test.server property must be defined in integration mode")
//
//  println(realUri)
//
//  def baseApiUri: String = realUri.getOrElse(mockUri)
//
//  "Data Application" should {
//    // wait while all data loaded into elastic
//    "init service" in {
//      go to s"$baseApiUri/v1/search/BusinessName:*"
//      Thread.sleep(1000)
//    }
//
//    // following tests rely on @InsertDemoData -
//    // local elastic with few loaded records
//    "search for any data" in {
//      go to s"$baseApiUri/v1/search/BusinessName:TORUS*"
//      pageSource must include("TORUS DEVELOPMENT CONSULTANTS LIMITED")
//    }
//
//    "get by id" in {
//      val id = 21840175L
//      go to s"$baseApiUri/v1/business/$id"
//      val rec = Json.fromJson[BusinessIndexRec](Json.parse(pageSource)).get
//      rec.id mustBe id
//    }
//
//    "search with limit" in {
//      val limit = 5
//      go to s"$baseApiUri/v1/search/BusinessName:*?limit=$limit"
//      extractData(pageSource).size mustBe limit
//    }
//
//    "check if only allowed fields" in {
//      go to s"$baseApiUri/v1/search/BusinessName:*?limit=1"
//      val rec = extractFirstData(pageSource)
//      rec.vatRefs mustBe None
//      rec.payeRefs mustBe None
//      rec.employmentBands mustNot be(None)
//      rec.companyNo mustNot be(None)
//    }
//
//    "search for special chars" in {
//      go to s"$baseApiUri/v1/search/BusinessName:" + "$$$"
//      val rec = extractFirstData(pageSource)
//      rec.companyNo mustBe Some("OH989326")
//    }
//
//    "check if industry code normalized" in {
//      go to s"$baseApiUri/v1/search/IndustryCode:742?limit=1"
//      val rec = extractFirstData(pageSource)
//      rec.industryCode mustBe Some("00742")
//    }
//
//    "check if company no non empty" in {
//      go to s"$baseApiUri/v1/search/IndustryCode:742?limit=1"
//      val rec = extractFirstData(pageSource)
//      rec.companyNo mustBe Some("LX756748")
//    }
//
//    "check if elasticsearch analyzers works" in {
//      val name = "GRAFIXSTAR LTD. CORP'S & BRTHR'S"
//
//      def checkFor(s: String) = {
//        go to s"$baseApiUri/v1/search/BusinessName:$s"
//        val res = extractFirstData(pageSource)
//        res.businessName mustBe name
//      }
//      checkFor(name)
//      def toSearch(in: String): String = {
//        in.lastIndexOf(' ') match {
//          case x if x > 0 =>
//            val l = in.substring(0, x)
//            checkFor(l)
//            toSearch(l)
//          case _ => ""
//        }
//      }
//      toSearch(name)
//    }
//
//    "check if exact postcode search returns only one result and is correct" in {
//      val postcode = "SE13 6AS"
//      go to s"$baseApiUri/v1/search/PostCode:($postcode)"
//      val res = extractData(pageSource)
//      res.length mustBe 1
//      res.head.postCode.getOrElse(sys.error(s"Postcode $postcode is empty in sample data")) mustBe postcode
//    }
//
//    "check if wildcard postcode search works correctly" in {
//      val postcode = "SE13"
//      go to s"$baseApiUri/v1/search/PostCode:($postcode)"
//      val res = extractData(pageSource)
//      res.length mustBe 4
//    }
//
//    "empty results returns properly" in {
//      go to s"$baseApiUri/v1/search/PostCode:UNEXISTED"
//      val res = pageSource
//      res must be("[]")
//    }
//
//    "invalid search should not generate exception" in {
//      go to s"$baseApiUri/v1/search/PostCode:^&%?fail_on_bad_query=false"
//      pageSource must include(""""ES could not execute query"""")
//    }
//
//  }
//
//  private def extractData(s: String) = Json.fromJson[List[BusinessIndexRec]](Json.parse(s)).get
//
//  private def extractFirstData(s: String) = extractData(s).headOption.getOrElse(sys.error(s"no business data returned by elastic: $s"))
//}
