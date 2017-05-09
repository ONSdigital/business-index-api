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

    "be able to take valid Json object and be parsable" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", Some(List(898989898989L, 111189898989L)), Some("BusinessName:test&limit1000"), "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      val eol = System.lineSeparator()
      contentAsString(feedback) must include(s"UBRN(s) of 898989898989, 111189898989${eol}and with query of BusinessName:test&limit1000")
    }

    "accept a data issue without a query param" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), None, "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("with UBRN(s) of 898989898989")
      contentAsString(feedback) mustNot include("query")
    }

    "accept a ui issue feedback without UBRN and query" in {
      val testObj = FeedbackObj("doej", "John Doe", "01/01/2000", "Data Issue", None, None, "UBRN does not match given company name.")
      val feedback = fakeRequest(uri, testObj)
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) mustNot include("ubrn" )
    }

    "successfully parse json string to object" in {
      val jsonString = """{ "username":"doej", "name":"John Doe", "date":"01/01/2000" , "subject":"Data Issue", "ubrn": [898989898989], "query": "BusinessName:test&limit=s1000", "comments":"UBRN does not match given company name."}"""
      val feedback = route(app, FakeRequest(POST, uri).withTextBody(jsonString)).getOrElse(sys.error(s"Cannot find route $uri."))
      status(feedback) mustBe(OK)
      contentType(feedback) mustBe Some("text/plain")
      contentAsString(feedback) must include("Feedback About Business Index")

    }

    "fail to parse json string to object" in {
      val jsonString = """{ "username":"doej", "date":"01/01/2000" , "subject":"Data Issue", "ubrn": [898989898989], "query": "BusinessName:test&limit=1000", "comments":"UBRN does not match given company name."}"""
      val feedback = route(app, FakeRequest(POST, uri).withTextBody(jsonString)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(feedback) must include("Invalid Feedback")
      status(feedback) mustBe(BAD_REQUEST)
    }

  }
}
