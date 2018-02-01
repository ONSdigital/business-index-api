package uk.gov.ons.bi.writers.indexes

import com.sksamuel.elastic4s.ElasticDsl.{ field, mapping }
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.mappings.MappingDefinition
import uk.gov.ons.bi.models.BIndexConsts._
import uk.gov.ons.bi.writers.Initializer

/**
 * Created by Volodymyr.Glushak on 16/02/2017.
 */
class BusinessIndex(val indexName: String) extends Initializer {

  override def recordName: String = cBiType

  override def analyzer: Option[AnalyzerDefinition] = Some(
    CustomAnalyzerDefinition(
      analyzerName,
      WhitespaceTokenizer,
      LowercaseTokenFilter
    )
  )

  /**
   * Uses the Elastic4S client DSL to build a specification for a given index.
   * This will basically use a generic index construction mechanism to pre-build
   * the indexes that already exist at the time when the Spark application is executed.
   *
   * @return A mapping definition.
   */
  override def indexDefinition: MappingDefinition = mapping(recordName).fields(
    field(cBiName, StringType) boost 4 analyzer analyzerName,
    field(cBiNameSuggest, CompletionType),

    field(cBiUprn, LongType) analyzer KeywordAnalyzer,

    field(cBiPostCode, StringType) analyzer analyzerName,

    field(cBiIndustryCode, LongType) analyzer KeywordAnalyzer,

    field(cBiLegalStatus, StringType) index "not_analyzed" includeInAll false,
    field(cBiTradingStatus, StringType) index "not_analyzed" includeInAll false,

    field(cBiTurnover, StringType) analyzer analyzerName,

    field(cBiEmploymentBand, StringType) analyzer analyzerName,

    field(cBiPayeRefs, StringType) analyzer KeywordAnalyzer,
    field(cBiVatRefs, LongType) analyzer KeywordAnalyzer,
    field(cBiCompanyNo, StringType) analyzer KeywordAnalyzer

  )
}

object BusinessIndex {
  def apply(name: String): BusinessIndex = new BusinessIndex(name)
}
