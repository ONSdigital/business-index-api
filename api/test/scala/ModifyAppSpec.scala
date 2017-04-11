package scala

import java.io.File

import controllers.v1.BusinessIndexObj._
import controllers.v1.OpStatus
import controllers.v1.OpStatus._
import org.apache.commons.io.FileUtils
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Writeable
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.FakeMultipartUpload._
import scala.concurrent.Future

/**
  * Created by Volodymyr.Glushak on 07/04/2017.
  */
class ModifyAppSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {


  private[this] def doDelete(uri: String) = doRequest(FakeRequest("DELETE", uri))

  private[this] def doRequest[T](request: Request[T])(implicit w: Writeable[T]): Future[Result] = route(app, request).getOrElse(sys.error("can't get response"))

  "Index modification" should {

    "store new, update and then remove it" in {
      val bir = BusinessIndexRec(90001, "name", None, None, None, None, None, None, None, None, None, None)
      val response = doRequest(FakeRequest("PUT", "/v1/store").withBody(Json.toJson(bir).toString()))
      status(response) mustBe OK
      fromJson(contentAsString(response)) mustBe opCreate("90001", true)

      val response0 = doRequest(FakeRequest("PUT", "/v1/store").withJsonBody(Json.toJson(bir)))
      status(response0) mustBe OK
      fromJson(contentAsString(response0)) mustBe opUpdate("90001", true)

      val response01 = doRequest(FakeRequest("PUT", "/v1/store").withTextBody(Json.toJson(bir).toString))
      status(response01) mustBe OK
      fromJson(contentAsString(response01)) mustBe opUpdate("90001", true)

      val response2 = doDelete("/v1/delete/90001")
      fromJson(contentAsString(response2)) mustBe opDelete("90001", true)

      val response3 = doDelete("/v1/delete/90001")
      fromJson(contentAsString(response3)) mustBe opDelete("90001", false) // already removed.
    }


    "bulk update" in {
      val fName = "the.temp.file"
      FileUtils.copyFile(new File("test/resources/modify.txt"), new File(fName))
      val ffile = Files.TemporaryFile(new File(fName))
      val part = FilePart[Files.TemporaryFile](key = "thekey", filename = fName, contentType = None, ref = ffile)
      val request = FakeRequest("POST", "/v1/update/bulk").
        withMultipartFormDataBody(MultipartFormData[Files.TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))
      val res = OpStatus.listFromJson(contentAsString(doRequest(request)))
      res.size mustBe 3
    }


  }

}