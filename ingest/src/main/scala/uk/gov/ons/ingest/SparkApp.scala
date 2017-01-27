package uk.gov.ons.ingest

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.ConfigFactory
import org.elasticsearch.common.settings.Settings
import uk.gov.ons.ingest.writers.indexes.{CompaniesHouseIndex, PayeIndex, VatIndex}

object SparkApp extends App {

  val config = ConfigFactory.load().getConfig("env.dev4")

  val elasticClusterUrl = config.getString("elasticsearch.uri")
  val sparkMaster = config.getString("spark.master")

  val elastic = ElasticClient.transport(
    Settings.settingsBuilder()
      .put("cluster.name", elasticClusterUrl)
      .put("client.transport.sniff", config.getBoolean("elasticsearch.client.transport.sniff"))
      .build(),
    ElasticsearchClientUri(elasticClusterUrl)
  )

  val ingestion = new SparkIngestion(
    elastic,
    VatIndex() :: PayeIndex() :: CompaniesHouseIndex() :: Nil
  )

}
