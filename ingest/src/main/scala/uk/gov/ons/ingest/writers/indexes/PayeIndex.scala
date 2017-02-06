package uk.gov.ons.ingest.writers.indexes

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import uk.gov.ons.ingest.writers.Initializer


class PayeIndex extends Initializer {

  override def recordName: String = "paye_receord"

  /**
    * The name of the given Elastic index.
    *
    * @return A string holding the index name. This will need to be unique and an error will occur
    *         at runtime when the application lifecycle attempts to create the index.
    */
  override def indexName: String = "paye_index"

  /**
    * Uses the Elastic4S client DSL to build a specification for a given index.
    * This will basically use a generic index construction mechanism to pre-build
    * the indexes that already exist at the time when the Spark application is executed.
    *
    * @return A mapping definition.
    */
  override def indexDefinition: MappingDefinition = mapping(recordName).fields(
    field("emp_stats_rec_id", StringType) ,
    field("district_number", IntegerType) boost 4 analyzer analyzerName,
    field("employer_reference", IntegerType),
    field("accounts_office_reference", IntegerType),

    field("address_1", StringType) index "not_analyzed" includeInAll false,
    field("address_2", StringType) index "not_analyzed" includeInAll false,
    field("address_3", DateType) index "not_analyzed" includeInAll false,
    field("address_4", StringType) index "not_analyzed" includeInAll false,
    field("address_5", StringType) index "not_analyzed" includeInAll false,
    field("postcode", StringType) index "not_analyzed" includeInAll false,

    field("trading_as_name_1", StringType) index "not_analyzed" includeInAll false,
    field("trading_as_name_2", StringType) index "not_analyzed" includeInAll false,
    field("communication_name", StringType) index "not_analyzed" includeInAll false,

    field("communication_address_1", StringType) index "not_analyzed" includeInAll false,
    field("communication_address_2", StringType) index "not_analyzed" includeInAll false,
    field("communication_address_3", DateType) index "not_analyzed" includeInAll false,
    field("communication_address_4", StringType) index "not_analyzed" includeInAll false,
    field("communication_address_5", StringType) index "not_analyzed" includeInAll false,
    field("communication_postcode", StringType) index "not_analyzed" includeInAll false,
    field("legal_status", StringType) index "not_analyzed" includeInAll false,
    field("scheme_type", StringType) index "not_analyzed" includeInAll false,


    field("date_commencted", DateType) index "not_analyzed" includeInAll false,
    field("transfer_in_identifier", StringType) index "not_analyzed" includeInAll false,
    field("successor_in_identifier", StringType) index "not_analyzed" includeInAll false,
    field("date_ceased", DateType) index "not_analyzed" includeInAll false,
    field("transfer_out_identifier", StringType) index "not_analyzed" includeInAll false,
    field("merger_out_identifier", StringType) index "not_analyzed" includeInAll false,
    field("succession_out_identifier", StringType) index "not_analyzed" includeInAll false,
    field("date_of_transfer", DateType) index "not_analyzed" includeInAll false,
    field("scheme_cancelled_date", DateType) index "not_analyzed" includeInAll false,
    field("scheme_reopened_date", DateType) index "not_analyzed" includeInAll false,
  )
}


object PayeIndex {
  def apply(): PayeIndex = new PayeIndex
}