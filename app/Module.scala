import com.google.inject.AbstractModule
import java.time.Clock

import services.{ApplicationTimer, AtomicCounter, Counter}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.StopAnalyzer
import scala.concurrent.duration._
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient

class Module extends AbstractModule {

  override def configure() = {
    val client = ElasticClient.local

    client.execute {
      create index "bi" mappings (
        "businesses" as (
          "id" typed IntegerType,
          "name" typed StringType boost 4
          )
        )
    }

    client.execute {
      index into "bi" / "businesses" id "1" fields (
        "name" -> "SomeBusiness Ltd"
        )
    }

    client.execute {
      index into "places" / "cities" id "2" fields (
        "name" -> "Alton Towers ltd"
        )
    }

    bind(classOf[ElasticClient]).toInstance(client)

    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])
  }

}
