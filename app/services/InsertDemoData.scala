package services

import java.io.FileNotFoundException
import javax.inject._

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl.{create, index}
import play.api.inject.ApplicationLifecycle
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import play.api.{Environment, Mode}

import scala.io.Source

/**
  * Class that imports sample.csv.
  *
  * CSV file header: "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
  */
@Singleton
class InsertDemoData @Inject()(environment: Environment, elasticSearch: ElasticClient, applicationLifecycle: ApplicationLifecycle) {
  elasticSearch.execute {
    // define the ElasticSearch index
    create.index("bi").mappings(
      mapping("business").fields(
        field("BusinessName", StringType) boost 4 analyzer "BusinessNameAnalyzer",
        field("UPRN", LongType) analyzer KeywordAnalyzer,
        field("IndustryCode", LongType) analyzer KeywordAnalyzer,
        field("LegalStatus", IntegerType) index "not_analyzed" includeInAll false,
        field("TradingStatus", IntegerType) index "not_analyzed" includeInAll false,
        field("Turnover", StringType) index "not_analyzed" includeInAll false,
        field("EmploymentBands", StringType) index "not_analyzed" includeInAll false
      )
    ).analysis(CustomAnalyzerDefinition("BusinessNameAnalyzer",
      StandardTokenizer("BusinessNameStandardTokenizer", 20),
      LowercaseTokenFilter,
      NGramTokenFilter("BusinessNameNgramFilter", minGram = 1, maxGram = 3)))
  }

  // if in dev mode, import the file sample.csv
  environment.mode match {
    case Mode.Dev =>
      readFile("/demo/sample.csv").filter(!_.contains("BusinessName")).foreach { line =>
        val values = line.replace("\"", "").split(",")

        elasticSearch.execute {
          index into "bi" / "business" id values(0) fields(
            "BusinessName" -> values(1),
            "UPRN" -> values(2).toLong,
            "IndustryCode" -> values(3).toLong,
            "LegalStatus" -> values(4).toInt,
            "TradingStatus" -> values(5).toInt,
            "Turnover" -> values(6),
            "EmploymentBands" -> values(7))
        }
      }

      println("Inserted DEMO data.")

      applicationLifecycle.addStopHook { () =>
        elasticSearch.execute {
          delete index "bi"
        }
      }
    case Mode.Test =>
    case Mode.Prod =>
  }

  def readFile(p: String): List[String] =
    Option(getClass.getResourceAsStream(p)).map(Source.fromInputStream)
      .map(_.getLines.toList)
      .getOrElse(throw new FileNotFoundException(p))
}
