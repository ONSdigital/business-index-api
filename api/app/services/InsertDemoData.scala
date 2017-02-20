package services

import javax.inject._

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException
import play.api.inject.ApplicationLifecycle
import uk.gov.ons.bi.ingest.helper.Utils
import uk.gov.ons.bi.ingest.parsers.CsvProcessor
import uk.gov.ons.bi.models.{BIndexConsts, BusinessIndexRec}
import uk.gov.ons.bi.writers.ElasticImporter

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Class that imports sample.csv.
  *
  * CSV file header: "ID","BusinessName","UPRN","IndustryCode","LegalStatus","TradingStatus","Turnover","EmploymentBands"
  */
@Singleton
class InsertDemoData @Inject()(applicationLifecycle: ApplicationLifecycle)(
  implicit exec: ExecutionContext,
  config: Config,
  elastic: ElasticClient
) extends StrictLogging {

  val elasticImporter = new ElasticImporter

  // TODO: must go to bi-data
  private[this] val businessIndex = config.getString("elasticsearch.bi.name")
  private[this] val initialization = config.getString("elastic.recreate.index").toBoolean

  private[this] def csv(p: String): Option[Iterator[String]] =
    Option(Utils.getResource(p)).map(_.filterNot(_.contains("BusinessName")))


  def initialiseIndex: Future[Any] = {
    if (initialization) {
      elasticImporter.initializeIndex(businessIndex)
    } else {
      Future.successful()
    }
  }

  def generateData: Iterator[BusinessIndexRec] =
    CsvProcessor.csvToMap(Utils.getResource("/demo/sample.csv")).map { r =>
      // PostCode is not in sample data ...
      BusinessIndexRec.fromMap(r("ID").toLong, Map(BIndexConsts.BiPostCode -> "SE") ++ r)
    }

  def importData(source: Iterator[BusinessIndexRec]): Future[Iterator[BulkResult]] = {
    logger.info(s"Starting to import the data, found elements to import: ${source.nonEmpty}")

    elastic.execute {
      search.in(businessIndex / "business")
    } flatMap {
      case resp if resp.hits.length == 0 => elasticImporter.loadBusinessIndex(businessIndex, source.toSeq)
      case resp@_ =>
        logger.info(s"No import necessary, found ${resp.hits.length} entries in the index")
        Future.successful(Iterator.empty)
    }
  }

  def init: Future[Iterator[BulkResult]] = {
    for {
      _ <- initialiseIndex recoverWith {
        case _: IndexAlreadyExistsException => {
          logger.info(s"Index $businessIndex already found in ")
          Future.successful(Nil)
        }
        case e: RemoteTransportException => {
          logger.error("Failed to connect to to ElasticSearch cluster", e)
          Future.failed(e)
        }
      }
      data <- importData(generateData)
    } yield data
  }

  logger.info("Importing sample data in all modes.")
  /* The behaviour is currently the same for all modes, match not required.
  // Leaving this here to be uncommented once we have real data to look at.
  environment.mode match {
    case Mode.Dev | Mode.Test => Try(Await.result(init, 5.minutes))
    case Mode.Prod =>
  }*/

  logger.info("InsertDemo Data service triggered")

  Try(Await.result(initialiseIndex, 2.minutes)) match {
    case Success(_) => logger.info(s"Initialised index $businessIndex")
    case Failure(err) =>
      logger.info(s"Index $businessIndex already exists, silenced error with ${err.getMessage}")
  }

  logger.info("Stating to import generated data")

  Try(Await.result(importData(generateData), 10.minutes)) match {
    case Success(_) => logger.info(s"Successfully imported data")
    case Failure(err) => logger.error("Unable to import generated data", err)
  }
}
