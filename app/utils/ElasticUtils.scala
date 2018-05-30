package utils

import javax.inject.Inject

import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, KeywordAnalyzer, LowercaseTokenFilter, WhitespaceTokenizer}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.index.{CreateIndexResponse, IndexResponse}
import com.sksamuel.elastic4s.http.{HttpClient, RequestFailure, RequestSuccess}
import config.ElasticSearchConfig
import models.IndexConsts._

/**
  * Created by coolit on 29/05/2018.
  */
class ElasticUtils @Inject() (elastic: HttpClient, config: ElasticSearchConfig) {

  def init(): Unit ={
    removeIndex()
    for {
      create <- createNewIndex().right
      insert <- insertTestData().right
    } yield insert
  }

  // We use Unit here as we don't care if the request fails
  def removeIndex(): Unit = {
    elastic.execute {
      deleteIndex(config.index)
    }.await
  }

  // Next, we need to create the index with the correct mappings/analyzers etc.
  // Changes to index creation:
  // - cannot use a KeywordAnalyzer on a long field
  // - using KeywordAnalyzer instead of NotAnalyzed - achieves the same thing?
  // - what does includeInAll do?
  def createNewIndex(): Either[RequestFailure, RequestSuccess[CreateIndexResponse]] = {
    elastic.execute {
      createIndex(config.index).analysis(
        CustomAnalyzerDefinition(cBiAnalyzer, WhitespaceTokenizer, LowercaseTokenFilter)
      ).mappings(
        mapping(cBiType).fields(
          textField(cBiName).boost(cBiNameBoost).analyzer(cBiAnalyzer),
          longField(cBiUprn),
          textField(cBiPostCode).analyzer(cBiAnalyzer),
          longField(cBiIndustryCode),
          textField(cBiLegalStatus).analyzer(KeywordAnalyzer),
          textField(cBiTradingStatus).analyzer(KeywordAnalyzer),
          textField(cBiTurnover).analyzer(cBiAnalyzer),
          textField(cBiEmploymentBand).analyzer(cBiAnalyzer),
          textField(cBiPayeRefs).analyzer(KeywordAnalyzer),
          longField(cBiVatRefs),
          textField(cBiCompanyNo).analyzer(KeywordAnalyzer)
        )
      )
    }.await
  }

  def insertTestData(): Either[RequestFailure, RequestSuccess[IndexResponse]] = {
    elastic.execute {
      indexInto(config.index / cBiType).id("12345678").fields(
        cBiName -> "Hello, world!"
      )
    }.await
  }
}
