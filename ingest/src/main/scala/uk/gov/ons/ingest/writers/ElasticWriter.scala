package uk.gov.ons.ingest.writers

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.elasticsearch.spark.sql._
import org.elasticsearch.spark._

class ElasticIndexes(config: Config) {
  final val payeIndex: String = config.getString("businessindex.elasticsearch.indices.paye")
  final val vatIndex: String = config.getString("businessindex.elasticsearch.indices.vat")
  final val charitiesComissionIndex: String = config.getString("businessindex.elasticsearch.indices.charities")
  final val companiesHouseRecords: String = config.getString("businessindex.elasticsearch.indices.companies")
}


/**
  * Contains methods that store supplied structures into ElasticSearch
  * These methods should contain side effects that store the info into
  * ElasticSearch without any additional business logic
  */
object ElasticWriter {

  private lazy val config = ConfigFactory.load()

  private lazy val hybridIndex = config.getString("businessindex.elasticsearch.indices.hybrid")

  /**
    * Stores data into ElasticSearch.
    * @param data `DataFrame` containing addresses
    */
  def save(index: String, data: DataFrame): Unit = data.saveToEs(index)

  /**
    * Stores arbitrary records
    */
  def save[T](index: String, data: RDD[T]): Unit = data.saveToEs(index)
}