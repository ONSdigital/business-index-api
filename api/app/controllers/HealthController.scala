package controllers

import play.api.mvc.Controller
import play.api.mvc._
import org.joda.time.DateTime

class HealthController extends Controller  {
  private[this] val startTime = System.currentTimeMillis()

  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime)+ "}").as(JSON)
  }

  private def uptime() : Long =  {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }
}
