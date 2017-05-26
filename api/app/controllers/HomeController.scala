package controllers

import org.joda.time.DateTime
import play.api.mvc.{Controller, _}
import io.swagger.annotations._

@Api("Utils")
class HomeController extends Controller {
  private[this] val startTime = System.currentTimeMillis()

  // public api
  @ApiOperation(value = "Swagger Documentation",
    notes = "Documentation of API endpoints for Swagger",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays swagger documentation.")
  ))
  def swagger = Action { request =>
    val host = request.host
    Redirect(s"http://$host/assets/lib/swagger-ui/index.html?/url=http://$host/swagger.json#/")
  }

  // public api
  @ApiOperation(value = "Application Health",
    notes = "Provides a json object containing minimal information on application live status and uptime.",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays a json object of basic api health.")
  ))
  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime) + "}")
  }

  private def uptime(): Long = {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }


  // public api
  @ApiOperation(value = "Permissions method request",
    notes = "preflight is used for local OPTIONS requests that precede PUT/DELETE requests. An empty Ok() response allows the actual PUT/DELETE request to be sent.",
    httpMethod = "OPTIONS")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Permission accepted with OK message"),
    new ApiResponse(code = 404, message = "Not Found - Root not Found"),
    new ApiResponse(code = 500, message = "Internal Server Error")))
  def preflight(all: String) = Action {
    Ok("")
  }

}
