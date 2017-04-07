package scala

import controllers.v1.BusinessIndexObj._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.concurrent.Future

/**
  * Created by Volodymyr.Glushak on 07/04/2017.
  */
class ModifyAppSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {


  private[this] def doRequest[T](request: Request[T])(implicit w: Writeable[T]): Future[Result] = route(app, request).getOrElse(sys.error("can't get response"))

  "Index modification" should {

    "store new, update and then remove it" in {
      val bir = BusinessIndexRec(90001, "name", None, None, None, None, None, None, None, None, None, None)
      val response = doRequest(FakeRequest("PUT", "/v1/store").withBody(Json.toJson(bir).toString()))
      status(response) mustBe OK
      contentAsString(response) mustBe "{'created'='true'}"

      val response0 = doRequest(FakeRequest("PUT", "/v1/store").withJsonBody(Json.toJson(bir)))
      status(response0) mustBe OK
      contentAsString(response0) mustBe "{'updated'='true'}"

      val response01 = doRequest(FakeRequest("PUT", "/v1/store").withTextBody(Json.toJson(bir).toString))
      status(response01) mustBe OK
      contentAsString(response01) mustBe "{'updated'='true'}"

      val response2 = doRequest(FakeRequest("GET", "/v1/delete/90001"))
      contentAsString(response2) mustBe "{'deleted'='true'}"

      val response3 = doRequest(FakeRequest("GET", "/v1/delete/90001"))
      contentAsString(response3) mustBe "{'deleted'='false'}" // already removed.


    }


  }

}
