import javax.inject.Inject

import ch.qos.logback.classic.LoggerContext
import com.codahale.metrics.JmxReporter
import com.codahale.metrics.health.HealthCheck
import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, _}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import nl.grons.metrics.scala.DefaultInstrumented
import org.elasticsearch.cluster.health.ClusterHealthStatus._
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment}
import services.InsertDemoData
import uk.gov.ons.bi.writers.{BiConfigManager, ElasticClientBuilder}

import scala.concurrent.Future

// see http://logback.qos.ch/manual/jmxConfig.html#leak
class AvoidLogbackMemoryLeak @Inject()(lifecycle: ApplicationLifecycle) extends StrictLogging {
  lifecycle.addStopHook { () =>
    logger.info("Shutting down logging context.")
    Future.successful(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop())
  }
}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with DefaultInstrumented with ElasticDsl {

  override def configure(): Unit = {

    val config = BiConfigManager.envConf(ConfigFactory.load())

    val elasticSearchClient = ElasticClientBuilder.build(config)

    bind(classOf[Config]).toInstance(config)

    bind(classOf[ElasticClient]).toInstance(elasticSearchClient)

    // register Elasticsearch cluster health check
    healthCheck("es-cluster-alive") {
      elasticSearchClient.execute {
        get cluster health
      }.await match {
        case response if response.getStatus == GREEN =>
          HealthCheck.Result.healthy(response.toString)
        case response =>
          HealthCheck.Result.unhealthy(response.toString)
      }
    }

    bind(classOf[InsertDemoData]).asEagerSingleton()
    bind(classOf[AvoidLogbackMemoryLeak]).asEagerSingleton()

    val reporter = JmxReporter.forRegistry(metricRegistry).build()
    reporter.start()
  }
}
