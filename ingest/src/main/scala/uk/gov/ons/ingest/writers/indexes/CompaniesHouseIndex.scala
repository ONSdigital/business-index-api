package uk.gov.ons.ingest.writers.indexes

import com.sksamuel.elastic4s.mappings.MappingDefinition
import uk.gov.ons.ingest.writers.Initializer
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import uk.gov.ons.ingest.writers.Initializer

class CompaniesHouseIndex extends Initializer {

  override def recordName: String = "companies_house_record"

  /**
    * The name of the given Elastic index.
    *
    * @return A string holding the index name. This will need to be unique and an error will occur
    *         at runtime when the application lifecycle attempts to create the index.
    */
  override def indexName: String = "companies_house_index"

  /**
    * Uses the Elastic4S client DSL to build a specification for a given index.
    * This will basically use a generic index construction mechanism to pre-build
    * the indexes that already exist at the time when the Spark application is executed.
    *
    * @return A mapping definition.
    */
  override def indexDefinition: MappingDefinition = mapping(recordName).fields(

  )
}

object CompaniesHouseIndex {
  def apply(): CompaniesHouseIndex = new CompaniesHouseIndex
}
