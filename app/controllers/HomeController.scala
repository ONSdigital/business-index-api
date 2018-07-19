package controllers

import org.joda.time.DateTime
import play.api.mvc.{ Controller, _ }
import io.swagger.annotations.Api

@Api("Utils")
class HomeController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  def swagger = Action { request =>
    val host = request.host
    Redirect(s"http://$host/assets/lib/swagger-ui/index.html?/url=http://$host/swagger.json#/")
  }

  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime) + "}")
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }

  /*
   * preflight is used for local OPTIONS requests that precede PUT/DELETE requests. An empty Ok() response allows
   * the actual PUT/DELETE request to be sent.
   */
  def preflight(all: String) = Action {
    Ok("")
  }

}
