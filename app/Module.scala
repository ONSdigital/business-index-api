import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import org.elasticsearch.common.settings.Settings
import play.api.{Configuration, Environment, Mode}
import services.InsertDemoData

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
  }
}