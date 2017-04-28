package scala

import controllers.v1.FeedbackObj
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{Json}
import play.api.test.Helpers.{contentAsString, _}
import play.api.test._

class FeedbackControllerSpec extends PlaySpec with GuiceOneAppPerTest {

    private[this] def fakeRequest(uri: String, testObj: FeedbackObj, method: String = POST) =
      route(app, FakeRequest(method, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Can not find route $uri."))

    val uri = "/v1/feedback"

  "FeedbackController" should {

    "passing valid Json object to be parsable" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", Some(898989898989L), Some("BusinessName:test&limit1000"), "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("query of BusinessName:test&limit1000\nand with UBRN of 898989898989")
    }

    "accepts a data issue without a query param" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "UI Issue", Some(898989898989L), None, "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("and with UBRN of 898989898989")
      contentAsString(feedback) mustNot include("query")
    }

    "accepts a ui issue feedback without UBRN and query" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", None, None, "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) mustNot include("ubrn" )
    }

    "invalid input forces exception" in {
//      val jsonString = "{ 'username':'doej', 'name':'John Doe', 'date':'01/01/2000' , 'subject':'Data Issue'', 'ubrn': '898989898989L', 'query': 'BusinessName:test&limit1000', 'comments':'UBRN does not match given company name.'}"
      val jsonString = """{ "username":"doej", "name":"John Doe", "date":"01/01/2000" , "subject":"Data Issue", "ubrn": "898989898989L", "query": "BusinessName:test&limit1000", "comments":"UBRN does not match given company name."}"""
      val jsonObj = Json.parse(jsonString)
//      val json: JsValue = Json.parse(jsonString)
//      val feedback = route(app, FakeRequest(POST, uri).withBody[JsValue](jsonObj)).getOrElse(sys.error(s"Cannot find route $uri."))
      val feedback = route(app, FakeRequest(POST, uri).withTextBody(jsonString)).getOrElse(sys.error(s"Cannot find route $uri."))
      println("FEEEDBACKKKK " + feedback)
      status(feedback) mustNot be(OK)
    }


  }
}
