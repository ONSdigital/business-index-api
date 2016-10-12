package services

import javax.inject._

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.{create, index}
import play.api.inject.ApplicationLifecycle
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

@Singleton
class InsertDemoData @Inject()(client: ElasticClient, appLifecycle: ApplicationLifecycle) {
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


  appLifecycle.addStopHook { () =>
    client.execute {
      delete index "bi"
    }
  }
}
