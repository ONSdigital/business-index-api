package uk.gov.ons.ingest

import akka.remote.RemoteTransportException
import com.sksamuel.elastic4s.ElasticClient
import org.apache.spark.SparkContext
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.slf4j.LoggerFactory
import uk.gov.ons.ingest.writers.Initializer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

case class SparkConfig(
  sparkMaster: String,
  hdfsUrl: String,
  elasticUrl: String,
  hbaseUrl: Option[String]
)

class SparkIngestion(
  val elastic: ElasticClient,
  val indexes: List[Initializer],
  val context: SparkContext
) {

  private[this] val timeoutSeconds = 20
  private[this] val logger = LoggerFactory.getLogger(getClass)

  protected[this] def shutdown(error: Throwable): Unit = {
    context.stop()
    sys.error(error.getMessage)
  }

  def setup(): Unit = {
    Await.result(
      Initializer(indexes: _*) recover {
        case e: IndexAlreadyExistsException => {
          logger.error("Index already exists, but the error can be silenced", e)
        }
        case x: RemoteTransportException => {
          logger.error("Could not connect to Elastic Cluster", x)
          shutdown(x)
        }
      },
      timeoutSeconds seconds
    )
  }
}
