package controllers

import java.util.concurrent.Executors

import com.google.common.base.Throwables
import com.google.common.util.concurrent.ThreadFactoryBuilder
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.mvc.Controller
import play.api.mvc._

import scala.collection.JavaConverters._

class HealthController extends Controller with DefaultInstrumented {
  private[this] val healthCheckExecutor = Executors.newFixedThreadPool(3,
    new ThreadFactoryBuilder().setNameFormat("health-check-%d").setDaemon(true).build())

  def health = Action {

    uk.gov.ons.bi.ingest.helper.Utils.Month

    val healthChecks = registry.runHealthChecks(healthCheckExecutor).asScala
    val hasUnhealthyCheck = healthChecks.forall(_._2.isHealthy)

    val body = healthChecks.map {
      case (key, result) if result.isHealthy =>
        healthBody(key, Option(result.getMessage).getOrElse("HEALTY"))
      case (key, result) if !result.isHealthy && result.getError == null =>
        healthBody(key, Option(result.getMessage).getOrElse("UNHEALTY"))
      case (key, result) if !result.isHealthy && result.getError != null =>
        healthBody(key, Option(result.getMessage).getOrElse("UNHEALTY"), Option(result.getError))
    }.mkString("{", ", ", "}")

    if (hasUnhealthyCheck) Ok(body).as(JSON) else ServiceUnavailable(body).as(JSON)
  }

  private def healthBody(key: String, message: String, error: Option[Throwable] = None): String = {
    error match {
      case Some(exception) =>
        s"\042$key\042:{\042message\042:\042$message\042,\042error\042:\042${Throwables.getStackTraceAsString(exception)}\042}"
      case None =>
        s"\042$key\042:\042$message\042"
    }
  }
}