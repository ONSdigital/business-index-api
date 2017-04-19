package services

import javax.inject._

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException
import play.api.inject.ApplicationLifecycle
import services.InsertDemoUtils._
import uk.gov.ons.bi.Utils
import uk.gov.ons.bi.CsvProcessor
import uk.gov.ons.bi.models.BusinessIndexRec
import uk.gov.ons.bi.writers.ElasticImporter

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

object InsertDemoUtils {

  val testFolder: String = sys.props.getOrElse("sample.folder", "demo")

  def generateData: Iterator[BusinessIndexRec] =
    CsvProcessor.csvToMap(Utils.getResource(s"/$testFolder/sample.csv")).map { r =>
      BusinessIndexRec.fromMap(r("ID").toLong, r)
    }
}

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
  private[this] val importSamples = config.getString("elastic.import.sample").toBoolean

  def initialiseIndex: Future[Option[CreateIndexResponse]] = {
    if (initialization) {
      elasticImporter.initializeIndex(businessIndex).map(cr => Option(cr))
    } else {
      Future.successful[Option[CreateIndexResponse]](None)
    }
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
    case Success(None) => logger.info(s"Recreation of index $businessIndex disabled!")
    case Success(Some(r)) =>
      logger.info(s"Initialised index $businessIndex: ${r.getHeaders.asScala}")
      // for local elastic version - CreateIndex is "partially async"
      // so when we're trying to import data its still doing some work (temp file creation maybe)
      Thread.sleep(100)
    case Failure(err) =>
      logger.info(s"Index $businessIndex already exists, silenced error with ${err.getMessage}")
  }

  if (importSamples) {
    logger.info("Stating to import generated data")

    Try(Await.result(importData(generateData), 10.minutes)) match {
      case Success(res) =>
        val ress = res.toList
        if (ress.exists(_.hasFailures))
          sys.error("Unexpected error while importing data:" + ress.map(_.failureMessage))
        logger.info(s"Successfully imported data")
      case Failure(err) => logger.error("Unable to import generated data", err)
    }
  }
}
