package services

import java.io.FileNotFoundException
import javax.inject._

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl.{create, index}
import play.api.inject.ApplicationLifecycle
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException
import play.api.{Environment, Mode}

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.control.NonFatal

/**
  * Class that imports sample.csv.
  *
  * CSV file header: "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
  */
@Singleton
class InsertDemoData @Inject()(environment: Environment,
                               elasticsearchClient: ElasticClient,
                               applicationLifecycle: ApplicationLifecycle)
                              (implicit exec: ExecutionContext) extends StrictLogging {

  elasticsearchClient.execute {
    // define the ElasticSearch index
    create.index(s"bi-${environment.mode.toString.toLowerCase}").mappings(
      mapping("business").fields(
        field("BusinessName", StringType) boost 4 analyzer "BusinessNameAnalyzer",
        field("BusinessName_suggest", CompletionType),
        field("UPRN", LongType) analyzer KeywordAnalyzer,
        field("IndustryCode", LongType) analyzer KeywordAnalyzer,
        field("LegalStatus", StringType) index "not_analyzed" includeInAll false,
        field("TradingStatus", StringType) index "not_analyzed" includeInAll false,
        field("Turnover", StringType) index "not_analyzed" includeInAll false,
        field("EmploymentBands", StringType) index "not_analyzed" includeInAll false
      )
    ).analysis(CustomAnalyzerDefinition("BusinessNameAnalyzer",
      StandardTokenizer,
      LowercaseTokenFilter,
      edgeNGramTokenFilter("BusinessNameNGramFilter") minGram 2 maxGram 24))
  }.recover {
    case _: IndexAlreadyExistsException => // Ok, ignore
    case _: RemoteTransportException => // Ok, ignore
  }.await

  if (environment.mode != Mode.Prod) {
    applicationLifecycle.addStopHook { () =>
      elasticsearchClient.execute {
        delete index s"bi-${environment.mode.toString.toLowerCase}"
      }
    }
  }

  // if in dev mode, import the file sample.csv
  environment.mode match {
    case Mode.Dev | Mode.Test =>
      var imported = 0
      readCSVFile(s"/${environment.mode.toString.toLowerCase}/sample.csv").foreach { case (line, lineNum) =>
        val values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map(_.replace("\"", ""))
        try {
          elasticsearchClient.execute {
            index into s"bi-${environment.mode.toString.toLowerCase}" / "business" id values(0) fields(
              "BusinessName" -> values(1),
              "UPRN" -> values(2).toLong,
              "IndustryCode" -> values(3).toLong,
              "LegalStatus" -> values(4),
              "TradingStatus" -> values(5),
              "Turnover" -> values(6),
              "EmploymentBands" -> values(7))
          }.await

          imported += 1
        }
        catch {
          case NonFatal(ex) => println(s"Failed importing line $lineNum: $line - ${ex}")
        }
      }

      logger.warn(s"Inserted ${environment.mode.toString.toLowerCase} data ($imported entries).")
    case Mode.Prod =>
  }

  private def readCSVFile(p: String): List[(String, Int)] =
    Option(getClass.getResourceAsStream(p)).map(Source.fromInputStream)
      .map(_.getLines.filterNot(_.contains("BusinessName")).zipWithIndex.toList)
      .getOrElse(throw new FileNotFoundException(p))
}
