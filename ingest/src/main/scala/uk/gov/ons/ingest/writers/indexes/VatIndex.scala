package uk.gov.ons.ingest.writers.indexes

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import uk.gov.ons.ingest.writers.Initializer

class VatIndex extends Initializer {
  override def indexName: String = "vat_index"

  override def recordName: String = "vat_record"

  override def analyzer: Option[AnalyzerDefinition] = Some(
    CustomAnalyzerDefinition(analyzerName,
    StandardTokenizer,
    LowercaseTokenFilter,
    edgeNGramTokenFilter(ngramName) minGram 2 maxGram 24)
  )

  override def indexDefinition: MappingDefinition = mapping(recordName).fields(
    field("lov_code", StringType) ,
    field("vat_registration_number", StringType) boost 4 analyzer analyzerName,
    field("trade_class", LongType),
    field("effective_date_of_registration", DateType),
    field("vat_1_date", DateType) index "not_analyzed" includeInAll false,
    field("insolvency_indicator", StringType) index "not_analyzed" includeInAll false,
    field("insolvency_date", DateType) index "not_analyzed" includeInAll false,
    field("group_division_indicator", StringType) index "not_analyzed" includeInAll false,
    field("voluntary_registration_indicator", StringType) index "not_analyzed" includeInAll false,
    field("intending_trader_indicator", StringType) index "not_analyzed" includeInAll false,
    field("status", StringType) index "not_analyzed" includeInAll false,
    field("part-exempt-indicator", StringType) index "not_analyzed" includeInAll false,
    field("stagger", IntegerType) index "not_analyzed" includeInAll false,
    field("repayment_indicator", IntegerType) index "not_analyzed" includeInAll false,
    field("total_turnover", LongType) index "not_analyzed" includeInAll false,
    field("trading_name", StringType) index "not_analyzed" includeInAll false,
    field("full_name", StringType) index "not_analyzed" includeInAll false,
    field("address_1", StringType) index "not_analyzed" includeInAll false,
    field("address_2", StringType) index "not_analyzed" includeInAll false,
    field("address_3", StringType) index "not_analyzed" includeInAll false,
    field("address_4", StringType) index "not_analyzed" includeInAll false,
    field("address_5", StringType) index "not_analyzed" includeInAll false,
    field("postcode", StringType) index "not_analyzed" includeInAll false
  )
}


object VatIndex {
  def apply(): VatIndex = new VatIndex
}