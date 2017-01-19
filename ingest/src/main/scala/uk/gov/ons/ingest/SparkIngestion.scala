package uk.gov.ons.ingest

import org.apache.spark.SparkContext


case class SparkConfig(
  hdfsUrl: String,
  elasticUrl: String,
  hbaseUrl: Option[String]
)

class SparkIngestion(
  val context: SparkContext
) {

  def setup: Unit = {
  }
}
