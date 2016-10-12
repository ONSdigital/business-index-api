package services

import javax.inject._

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl.{create, index}
import play.api.inject.ApplicationLifecycle
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

@Singleton
class InsertDemoData @Inject()(client: ElasticClient, appLifecycle: ApplicationLifecycle) {
  client.execute {
    // "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
    create.index("bi").mappings(
      "businesses" as(
        "id" typed LongType,
        "BusinessName" typed StringType boost 4 analyzer "BusinessNameAnalyzer",
        "UPRN" typed LongType,
        "IndustryCode" typed LongType,
        "LegalStatus" typed IntegerType,
        "TradingStatus" typed IntegerType,
        "TurnOver" typed StringType,
        "EmploymentBands" typed StringType
        )
    ).analysis(CustomAnalyzerDefinition("BusinessNameAnalyzer", WhitespaceTokenizer, LowercaseTokenFilter))
  }

  client.execute {
    index into "bi" / "businesses" id "84676015" fields(
      "BusinessName" -> "BIRKENSHAW DISTRIBUTORS LIMITED",
      "UPRN" -> 643596572265L,
      "IndustryCode" -> 94349,
      "LegalStatus" -> 5,
      "TradingStatus" -> 3,
      "Turnover" -> "D",
      "EmploymentBands" -> "K"
      )
  }

  println("Inserted DEMO data.")

  appLifecycle.addStopHook { () =>
    client.execute {
      delete index "bi"
    }
  }
}
