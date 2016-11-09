package controllers

import java.util.concurrent.Executors

import com.google.common.base.Throwables
import com.google.common.util.concurrent.ThreadFactoryBuilder
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.mvc.Controller
import play.api.mvc._

import scala.collection.JavaConverters._

class HealthController extends Controller with DefaultInstrumented {
  val healthCheckExecutor = Executors.newFixedThreadPool(3,
    new ThreadFactoryBuilder().setNameFormat("health-check-%d").setDaemon(true).build())

  def health = Action {
    val healthChecks = registry.runHealthChecks(healthCheckExecutor).asScala
    val hasUnhealthyCheck = healthChecks.forall(_._2.isHealthy)

    val body = healthChecks.map {
      case (key, result) if result.isHealthy =>
        s"\042$key\042:\042${Option(result.getMessage).getOrElse("HEALTHY")}\042"
      case (key, result) if !result.isHealthy && result.getError == null =>
        s"\042$key\042:\042${Option(result.getMessage).getOrElse("UNHEALTHY")}\042"
      case (key, result) if !result.isHealthy && result.getError != null =>
        s"\042$key\042:{\042message\042:\042${Option(result.getMessage).getOrElse("UNHEALTHY")}\042,\042error\042:\042${Option(Throwables.getStackTraceAsString(result.getError))}\042}"
    }.mkString("{", ", ", "}")

    if (hasUnhealthyCheck) Ok(body).as(JSON) else ServiceUnavailable(body).as(JSON)
  }
}