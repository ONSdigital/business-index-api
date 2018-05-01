import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.{ Inject, Singleton }
import java.security.cert.X509Certificate
import javax.net.ssl.{ SSLContext, X509TrustManager }
import org.elasticsearch.client.RestClientBuilder._
import org.apache.http.client.config.RequestConfig.Builder

import org.apache.http.impl.nio.client._
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.auth.AuthScope
import ch.qos.logback.classic.LoggerContext
import com.codahale.metrics.JmxReporter
import com.codahale.metrics.health.HealthCheck
import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import nl.grons.metrics.scala.DefaultInstrumented
import org.elasticsearch.client.RestClientBuilder.{ HttpClientConfigCallback, RequestConfigCallback }
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Environment }
//import services.InsertDemoData
import uk.gov.ons.bi.writers.{ BiConfigManager }

import scala.concurrent.Future

// see http://logback.qos.ch/manual/jmxConfig.html#leak
class AvoidLogbackMemoryLeak @Inject() (lifecycle: ApplicationLifecycle) extends StrictLogging {
  lifecycle.addStopHook { () =>
    logger.info("Shutting down logging context.")
    Future.successful(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop())
  }
}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with DefaultInstrumented with ElasticDsl {

  override def configure(): Unit = {

    val config: Config = BiConfigManager.envConf(ConfigFactory.load())

    // val elasticSearchClient = ElasticClientBuilder.build(config)

    val host = config.getString("elasticsearch.host")
    val port = config.getInt("elasticsearch.port")
    val suffix = config.getString("elasticsearch.suffix")
    val ssl = config.getBoolean("elasticsearch.ssl")

    //    val elasticSearchClient = HttpClient(ElasticsearchClientUri(s"elasticsearch://$host:$port$suffix"))

    lazy val provider = {
      logger info "Connecting to Elasticsearch"
      val provider = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials("elastic", "changeme")
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }

    val context = SSLContext.getInstance("SSL")
    context.init(null, Array(
      new X509TrustManager {
        def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
        def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
        def getAcceptedIssuers: Array[X509Certificate] = Array()
      }
    ), null)

    val elasticSearchClient = HttpClient(ElasticsearchClientUri(s"elasticsearch://$host:$port$suffix?ssl=$ssl"), new RequestConfigCallback {
      override def customizeRequestConfig(requestConfigBuilder: Builder) = {
        requestConfigBuilder
      }
    }, new HttpClientConfigCallback {
      override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder) = {
        httpClientBuilder.setDefaultCredentialsProvider(provider)
        httpClientBuilder.setSSLContext(context)
      }
    })

    bind(classOf[Config]).toInstance(config)

    bind(classOf[HttpClient]).toInstance(elasticSearchClient)

    //    // register Elasticsearch cluster health check
    //    healthCheck("es-cluster-alive") {
    //      elasticSearchClient.execute {
    //        get cluster health
    //      }.await match {
    //        case response if response.getStatus == GREEN =>
    //          HealthCheck.Result.healthy(response.toString)
    //        case response =>
    //          HealthCheck.Result.unhealthy(response.toString)
    //      }
    //    }

    //    bind(classOf[InsertDemoData]).asEagerSingleton()
    bind(classOf[AvoidLogbackMemoryLeak]).asEagerSingleton()

    //    if (!SingleJmxReporterPerJvm.initialized.getAndSet(true)) {
    //      val reporter = JmxReporter.forRegistry(metricRegistry).build()
    //      reporter.start()
    //    }
  }
}

object SingleJmxReporterPerJvm {

  val initialized = new AtomicBoolean(false)

}
