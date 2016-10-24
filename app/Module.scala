import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.common.settings.Settings
import play.api.{Configuration, Environment, Mode}
import services.InsertDemoData

class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {

  override def configure() = {
    val elasticSearchClient = environment.mode match {
      case Mode.Dev =>
        val settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch_" + System.getProperty("user.name")).build()
        val uri = ElasticsearchClientUri("elasticsearch://localhost:9300")
        ElasticClient.transport(settings, uri)
      case _ =>
        val settings = Settings.settingsBuilder().put("cluster.name", configuration.getString("elasticsearch.cluster.name").get).build()
        val uri = ElasticsearchClientUri(configuration.getString("elasticsearch.uri").get)
        ElasticClient.transport(settings, uri)
    }

    bind(classOf[ElasticClient]).toInstance(elasticSearchClient)
    bind(classOf[InsertDemoData]).asEagerSingleton()
  }
}