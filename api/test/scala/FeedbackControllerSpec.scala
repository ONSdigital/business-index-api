package scala

import controllers.v1.FeedbackObj
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{Json}
import play.api.test.Helpers.{contentAsString, _}
import play.api.test._

class FeedbackControllerSpec extends PlaySpec with GuiceOneAppPerTest {
  //
//    private[this] def fakeRequest(uri: String, testObj: Any, method: String = POST) =
//      route(app, FakeRequest(method, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Can not find route $uri."))

    val uri = "/v1/feedback"

  "FeedbackController" should {

    "passing valid Json object to be parsable" in {

      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", Some(898989898989L), Some("BusinessName:test&limit1000"), "UBRN does not match given company name.")
      //      val testObj = FeedbackObj("test", "test", "test", "test",  Option(898989898989L), Option("test"), "test")
      val feedback = route(app, FakeRequest(POST, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Cannot find route $uri."))
//      val feedback = fakeRequest(uri, testObj)

      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("query of BusinessName:test&limit1000\nand with UBRN of 898989898989")
    }

    "accepts a data issue without a query param" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "UI Issue", Some(898989898989L), None, "UBRN does not match given company name.")

      val feedback = route(app, FakeRequest(POST, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Cannot find route $uri."))
//      val feedback = fakeRequest(uri).withJsonBody(Json.toJson(testObj))
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("and with UBRN of 898989898989")
      contentAsString(feedback) mustNot include("query")
    }


    "accepts a ui issue feedback without ubrn and query" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", None, None, "UBRN does not match given company name.")
      val feedback = route(app, FakeRequest(POST, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Cannot find route $uri."))
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) mustNot include("ubrn" )
    }

//    "invalid input forces exception" in {
//      val uri = "/v1/feedback"
//      val testString: JsValue = Json.parse("[{ 'username':'John', 'name':'hello', 'date':'New York' , 'subject':'John', 'ubrn':317487345875837, 'query': 'New York', 'comments':'New York'}]")
//      val feedback = route(app, FakeRequest(POST, uri).withJsonBody(Json.toJson(testString))).getOrElse(sys.error(s"Cannot find route $uri."))
//      println("FEEEDBACKKKK " + feedback)
//      status(feedback) mustNot be(OK)
//    }


  }
}
