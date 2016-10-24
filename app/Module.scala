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
    val (settings, uri) = environment.mode match {
      case Mode.Dev =>
        (Settings.settingsBuilder().put("cluster.name", "elasticsearch_" + System.getProperty("user.name")).build(),
          ElasticsearchClientUri("elasticsearch://localhost:9300"))
      case _ =>
        (Settings.settingsBuilder().put("cluster.name", configuration.getString("elasticsearch.cluster.name").get).build(),
          ElasticsearchClientUri(configuration.getString("elasticsearch.uri").get))
    }

    bind(classOf[ElasticClient]).toInstance(ElasticClient.transport(settings, uri))
    bind(classOf[InsertDemoData]).asEagerSingleton()
    bind(classOf[AvoidLogbackMemoryLeak]).asEagerSingleton()
  }
}