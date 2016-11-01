import javax.inject.Inject

import ch.qos.logback.classic.LoggerContext
import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Environment, Mode}
import services.InsertDemoData

import scala.concurrent.Future

// see http://logback.qos.ch/manual/jmxConfig.html#leak
class AvoidLogbackMemoryLeak @Inject()(lifecycle: ApplicationLifecycle) extends StrictLogging {
  lifecycle.addStopHook { () =>
    logger.info("Shutting down logging context.")
    Future.successful(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop())
  }
}

class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

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
    bind(classOf[InsertDemoData]).asEagerSingleton()
    bind(classOf[AvoidLogbackMemoryLeak]).asEagerSingleton()
  }
}