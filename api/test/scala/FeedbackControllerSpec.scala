package scala


import controllers.v1.feedback.FeedbackObj
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, _}
import play.api.test._

import scala.service.HBaseTesting

class FeedbackControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  HBaseTesting.hBaseServer // to make sure hBaseServer initialized and can be used in the test.

  private[this] def fakeRequest(uri: String, testObj: FeedbackObj, method: String = POST) =
    route(app, FakeRequest(method, uri).withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Can not find route $uri."))


  private[this] def doRequest (uri: String, method: String = DELETE) = route(app, FakeRequest(method, uri)).getOrElse(sys.error("can't get response"))

  val uri = "/v1/feedback"

  def recordObj (username: String = "doej", name: String = "John Doe", subject: String = "Data Issue", ubrn: Option[List[Long]] = Some(List(898989898989L, 111189898989L)), query: Option[String] = Some("BusinessName:test&limit=100")) = FeedbackObj(None, username, name, Some("01:01:2000"), subject, ubrn, query, "UBRN does not match given company name." )


  val eol = System.lineSeparator()

  "FeedbackController" should {

    "accept feedback without date override" in {
      val testObj = FeedbackObj (id = None, username = "doej", name = "John Doe", date = None, subject = "UI Issue", ubrn = None, query = None, comments = "Just a simple test")
      val feedback =  route(app, FakeRequest(POST, "/v1/feedback").withJsonBody(Json.toJson(testObj))).getOrElse(sys.error(s"Can not find route $uri."))
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) must include ("doej")
    }

    "be able to take valid Json object and be parsable" in {
      val feedback = fakeRequest(uri, recordObj())
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) must include ("doej01:01:2000")
    }

    "accept a data issue without a query param" in {
      val feedback = fakeRequest(uri, recordObj(username = "coolit", name = "Tom Colling", ubrn = Some(List(117485788989L)), query = None))
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) must include ("doej01:01:2000")
    }

    "accept a ui issue feedback without UBRN and query" in {
      val feedback = fakeRequest(uri, recordObj(ubrn = None, query = None))
      status(feedback) mustBe OK
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) must include ("doej01:01:2000")
    }

    "successfully parse json string to object" in {
      val  recordJson = """{ "id":  "null", "username":"sonb", "name":"Bill Son", "subject":"Data Issue", "ubrn": [898989898989], "query": "BusinessName:test&limit=100", "comments":"This is just a test, please ignore."}"""
      val feedback = route(app, FakeRequest(POST, uri).withTextBody(recordJson)).getOrElse(sys.error(s"Cannot find route $uri."))
      status(feedback) mustBe(OK)
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) must include ("sonb")
    }

    "fail to parse json string to object" in {
      val jsonString = """{ "username":"doej", "date":"01:01:2000" , "subject":"Data Issue", "ubrn": [898989898989], "query": "BusinessName:test&limit=1000", "comments":"UBRN does not match given company name."}"""
      val feedback = route(app, FakeRequest(POST, uri).withTextBody(jsonString)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(feedback) must include("Invalid Feedback")
      status(feedback) mustBe(BAD_REQUEST)
    }

    "change the hide status of an existing record by id" in {
      val feedback = doRequest(uri + "/sonb01:01:2000" )
      status(feedback) mustBe(OK)
      contentType(feedback) mustBe Some("text/plain")
      val check = route(app, FakeRequest(GET, uri)).getOrElse(sys.error(s"Cannot find route $uri."))
      contentAsString(check) mustNot include ("Some(true)")
      contentAsString(check) mustNot include ("sonb01:01:2000")
    }

  }
}

