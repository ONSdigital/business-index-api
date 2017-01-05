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

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.concurrent.duration._
import scala.util.Try

/**
  * Class that imports sample.csv.
  *
  * CSV file header: "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
  */
@Singleton
class InsertDemoData @Inject()(
  environment: Environment,
  elasticsearchClient: ElasticClient,
  applicationLifecycle: ApplicationLifecycle
)(
  implicit exec: ExecutionContext
) extends StrictLogging {

  private[this] def readCSVFile(p: String): List[(String, Int)] =
    Option(getClass.getResourceAsStream(p)).map(Source.fromInputStream)
      .map(_.getLines.filterNot(_.contains("BusinessName")).zipWithIndex.toList)
      .getOrElse(throw new FileNotFoundException(p))

  def initialiseIndex: Future[Unit] = {
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
    } map { res =>
      if (environment.mode != Mode.Prod) {
        applicationLifecycle.addStopHook { () =>
          elasticsearchClient.execute {
            delete index s"bi-${environment.mode.toString.toLowerCase}"
          }
        }
      }
    }
  }

  def generateData(): List[Array[String]] = {
    readCSVFile(s"/${environment.mode.toString.toLowerCase}/sample.csv") map { case (line, lineNum) =>
      line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map(_.replace("\"", ""))
    }
  }

  def importData(source: List[Array[String]]): Future[List[IndexResult]] = {
    Future.sequence {
      source map { values =>
        elasticsearchClient.execute {
          index into s"bi-${environment.mode.toString.toLowerCase}" / "business" id values(0) fields(
            "BusinessName" -> values(1),
            "UPRN" -> values(2).toLong,
            "IndustryCode" -> values(3).toLong,
            "LegalStatus" -> values(4),
            "TradingStatus" -> values(5),
            "Turnover" -> values(6),
            "EmploymentBands" -> values(7))
        }
      }
    }
  }

  def init: Future[List[IndexResult]] = {
    for {
      _ <- initialiseIndex recoverWith {
        case _: IndexAlreadyExistsException => Future.successful(Nil)
        case e: RemoteTransportException => Future.failed(e)
      }
      data <- importData(generateData())
    } yield data
  }

  logger.info("Importing sample data in all modes.")
  /* The behaviour is currently the same for all modes, match not required.
  // Leaving this here to be uncommented once we have real data to look at.
  environment.mode match {
    case Mode.Dev | Mode.Test => Try(Await.result(init, 5.minutes))
    case Mode.Prod =>
  }*/

  Try(Await.result(init, 5.minutes))
}