package services

import java.io.FileNotFoundException
import javax.inject._

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl.{create, index}
import play.api.inject.ApplicationLifecycle
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, LowercaseTokenFilter, WhitespaceTokenizer}
import com.sksamuel.elastic4s.mappings.FieldType._

import scala.io.Source

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

  readResourceFile("/demo/sample.csv").filter(!_.contains("BusinessName")).foreach { line =>
    val values = line.split(",")

    client.execute {
      index into "bi" / "businesses" id values(0) fields(
        "BusinessName" -> values(1),
        "UPRN" -> values(2).toLong,
        "IndustryCode" -> values(3).toLong,
        "LegalStatus" -> values(4).toInt,
        "TradingStatus" -> values(5).toInt,
        "Turnover" -> values(6),
        "EmploymentBands" -> values(7))
    }
  }

  def readResourceFile(p: String): List[String] =
    Option(getClass.getResourceAsStream(p)).map(Source.fromInputStream)
      .map(_.getLines.toList)
      .getOrElse(throw new FileNotFoundException(p))

  println("Inserted DEMO data.")

  appLifecycle.addStopHook { () =>
    client.execute {
      delete index "bi"
    }
  }
}
