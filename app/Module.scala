import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.common.settings.Settings
import services.InsertDemoData

class Module extends AbstractModule {
  override def configure() = {
    val settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch_mathias").build()
    val uri = ElasticsearchClientUri("elasticsearch://localhost:9300")
    val client = ElasticClient.transport(settings, uri)

    bind(classOf[ElasticClient]).toInstance(client)
    bind(classOf[InsertDemoData]).asEagerSingleton()
  }
}