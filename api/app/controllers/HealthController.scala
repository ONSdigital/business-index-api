package controllers

import java.util.concurrent.Executors

import com.google.common.base.Throwables
import com.google.common.util.concurrent.ThreadFactoryBuilder
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.mvc.Controller
import play.api.mvc._

import scala.collection.JavaConverters._

import java.util.Date
import org.joda.time.DateTime




class HealthController extends Controller  {
  private[this] val startTime = System.currentTimeMillis()

  def health = Action {
    val uptimeInMillis = uptime()
    Ok(s"{Status: Ok, Uptime: ${uptimeInMillis}ms, Date and Time: " + new DateTime(startTime)+ "}")
  }

  private def uptime() : Long =  {
    val uptimeInMillis = System.currentTimeMillis() - startTime
    uptimeInMillis
  }

}
