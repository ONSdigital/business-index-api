import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import java.security.cert.X509Certificate
import javax.net.ssl.{ SSLContext, X509TrustManager }

import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.impl.nio.client._
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.auth.AuthScope
import ch.qos.logback.classic.LoggerContext
import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.client.RestClientBuilder.{ HttpClientConfigCallback, RequestConfigCallback }
import org.slf4j.LoggerFactory
import play.api.inject.ApplicationLifecycle
import play.api.{ Configuration, Environment }
import services.BusinessService
import repository.ElasticSearchBusinessRepository
import utils.ElasticRequestMapper

import scala.concurrent.Future

import config.ElasticSearchConfigLoader

// see http://logback.qos.ch/manual/jmxConfig.html#leak
class AvoidLogbackMemoryLeak @Inject() (lifecycle: ApplicationLifecycle) extends StrictLogging {
  lifecycle.addStopHook { () =>
    logger.info("Shutting down logging context.")
    Future.successful(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].stop())
  }
}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with ElasticDsl {

  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val elasticConfig = ElasticSearchConfigLoader.load(underlyingConfig)

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

    val elasticSearchClient = HttpClient(ElasticsearchClientUri(
      s"elasticsearch://${elasticConfig.host}:${elasticConfig.port}?ssl=${elasticConfig.ssl}"
    ), new RequestConfigCallback {
      override def customizeRequestConfig(requestConfigBuilder: Builder) = {
        requestConfigBuilder
      }
    }, new HttpClientConfigCallback {
      override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder) = {
        httpClientBuilder.setDefaultCredentialsProvider(provider)
        httpClientBuilder.setSSLContext(context)
      }
    })

    val elasticSearchBusinessRepository = new ElasticSearchBusinessRepository(elasticSearchClient, new ElasticRequestMapper, elasticConfig)

    bind(classOf[BusinessService]).toInstance(elasticSearchBusinessRepository)
    bind(classOf[AvoidLogbackMemoryLeak]).asEagerSingleton()
  }
}

object SingleJmxReporterPerJvm {

  val initialized = new AtomicBoolean(false)

}
