import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.ElasticClient
import services.InsertDemoData

class Module extends AbstractModule {

  override def configure() = {
    val client = ElasticClient.local
    bind(classOf[ElasticClient]).toInstance(client)

    bind(classOf[InsertDemoData]).asEagerSingleton()
  }
}
