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
import scala.util.{Failure, Success, Try}

/**
  * Class that imports sample.csv.
  *
  * CSV file header: "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
  */
@Singleton
class InsertDemoData @Inject()(
  environment: Environment,
  elastic: ElasticClient,
  applicationLifecycle: ApplicationLifecycle
)(
  implicit exec: ExecutionContext
) extends StrictLogging {

  private[this] val envString = environment.mode.toString.toLowerCase

  private[this] val businessIndex = s"bi-$envString"

  private[this] def csv(p: String): Option[Iterator[String]] =
    Option(getClass.getResourceAsStream(p)).map(Source.fromInputStream)
      .map(_.getLines.filterNot(_.contains("BusinessName")))


  def initialiseIndex: Future[Unit] = {
    elastic.execute {
      // define the ElasticSearch index
      create.index(businessIndex).mappings(
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
    } map { _ =>
      if (environment.mode != Mode.Prod) {
        applicationLifecycle.addStopHook { () =>
          elastic.execute { delete index s"bi-$envString"}
        }
      }
    }
  }

  def generateData(): Iterator[Array[String]] = {

    val dataSource = (csv(s"/$envString/sample.csv") orElse csv("/sample.csv")) getOrElse {
      val error = new FileNotFoundException("sample.csv")
      logger.error("Unable to find any sample.csv file in the classpath", error)
      throw error
    }

    dataSource map {
      _.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map(_.replace("\"", ""))
    }
  }

  def importData(source: Iterator[Array[String]]): Future[Iterator[IndexResult]] = {
    Console.println(s"Starting to import the data, found elements to import: ${source.nonEmpty}")

    val importFuture = Future.sequence {
      source map { values =>
        elastic.execute {
          logger.debug("Indexing entry in ElasticSearch")
          index into businessIndex / "business" id values(0) fields(
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

    elastic.execute {
      search.in(businessIndex / "business")
    } flatMap {
      case resp if resp.hits.length == 0 => importFuture
      case resp @ _ => {
        Console.println(s"No import necessary, found ${resp.hits.length} entries in the index")
        Future.successful(Iterator.empty)
      }
    }
  }

  def init: Future[Iterator[IndexResult]] = {
    for {
      _ <- initialiseIndex recoverWith {
        case _: IndexAlreadyExistsException => {
          Console.println(s"Index $businessIndex already found in ")
          Future.successful(Nil)
        }
        case e: RemoteTransportException => {
          Console.println("Failed to connect to ElasticSearch ")
          Console.println(e.getStackTraceString)
          Future.failed(e)
        }
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

  Console.println("InsertDemo Data service triggered")

  Try(Await.result(initialiseIndex, 2.minutes)) match {
    case Success(_) => Console.println(s"Initialised index $businessIndex")
    case Failure(err) => Console.println(err.getStackTraceString)
  }

  Console.println("Stating to import generated data")

  Try(Await.result(importData(generateData()), 10.minutes)) match {
    case Success(_) => Console.println(s"Successfully imported data")
    case Failure(err) => Console.println(err.getStackTraceString)
  }
}
