import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.analyzers._
import play.api.{ Configuration, Environment }
import services.BusinessRepository
import repository.ElasticSearchBusinessRepository
import utils.{ ElasticClient, ElasticResponseMapper, ElasticResponseMapperSecured }
import config.ElasticSearchConfigLoader
import models.IndexConsts._

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    import com.sksamuel.elastic4s.http.ElasticDsl._

    val underlyingConfig = configuration.underlying
    val elasticConfig = ElasticSearchConfigLoader.load(underlyingConfig)
    val elasticSearchClient = ElasticClient.getElasticClient(elasticConfig)
    val elasticSearchBusinessRepository = new ElasticSearchBusinessRepository(
      elasticSearchClient, new ElasticResponseMapper, new ElasticResponseMapperSecured, elasticConfig
    )

    // Need to use config to choose whether or not to create index again etc.

    // Firstly, we need to delete the index (we don't care if this fails)
    elasticSearchClient.execute {
      deleteIndex(elasticConfig.index)
    }.await

    // Next, we need to create the index with the correct mappings/analyzers etc.
    // Changes to index creation:
    // - cannot use a KeywordAnalyzer on a long field
    // - using KeywordAnalyzer instead of NotAnalyzed - achieves the same thing?
    // - what does includeInAll do?
    val a = elasticSearchClient.execute {
      createIndex(elasticConfig.index).analysis(
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

    // Now we can insert some test data
    elasticSearchClient.execute {
      indexInto(elasticConfig.index / cBiType).id("12345678").fields(
        cBiName -> "Hello, world!"
      )
    }

    bind(classOf[BusinessRepository]).toInstance(elasticSearchBusinessRepository)
  }
}
