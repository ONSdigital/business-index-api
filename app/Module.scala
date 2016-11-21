import javax.inject.Inject

import ch.qos.logback.classic.LoggerContext
import com.codahale.metrics.JmxReporter
import com.codahale.metrics.health.HealthCheck
import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.scalalogging.StrictLogging
import nl.grons.metrics.scala.DefaultInstrumented
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}
import services.InsertDemoData
import com.sksamuel.elastic4s._
import org.elasticsearch.cluster.health.ClusterHealthStatus._

import scala.concurrent.Future

// see http://logback.qos.ch/manual/jmxConfig.html#leak
class AvoidLogbackMemoryLeak @Inject()(lifecycle: ApplicationLifecycle) extends StrictLogging {
  lifecycle.addStopHook { () =>
    logger.info("Shutting down logging context.")
    Future.successful(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop())
  }
}

class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule with DefaultInstrumented with ElasticDsl {

  override def configure() = {
    val elasticSearchClient = environment.mode match {
      case Mode.Dev =>
        ElasticClient.transport(
          Settings.settingsBuilder()
            .put("cluster.name", "elasticsearch_" + System.getProperty("user.name"))
            .put("client.transport.sniff", true)
            .build(),
          ElasticsearchClientUri("elasticsearch://localhost:9300")
        )
      case Mode.Test =>
        ElasticClient.local(
          Settings.settingsBuilder()
            .put("path.home", System.getProperty("java.io.tmpdir"))
            .put("client.transport.sniff", true)
            .build()
        )
      case _ =>
        ElasticClient.transport(
          Settings.settingsBuilder()
            .put("cluster.name", configuration.getString("elasticsearch.cluster.name").get)
            .put("client.transport.sniff", configuration.getBoolean("elasticsearch.client.transport.sniff").getOrElse(true))
            .build(),
          ElasticsearchClientUri(configuration.getString("elasticsearch.uri").get)
        )
    }

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
