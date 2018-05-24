import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl
import play.api.{Configuration, Environment}
import services.BusinessRepository
import repository.ElasticSearchBusinessRepository
import utils.{ElasticClient, ElasticResponseMapper, ElasticResponseMapperSecured}
import config.ElasticSearchConfigLoader
import models.IndexConsts._

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with ElasticDsl {

  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val elasticConfig = ElasticSearchConfigLoader.load(underlyingConfig)
    val elasticSearchClient = ElasticClient.getElasticClient(elasticConfig)
    val elasticSearchBusinessRepository = new ElasticSearchBusinessRepository(
      elasticSearchClient, new ElasticResponseMapper, new ElasticResponseMapperSecured, elasticConfig
    )

    // Need to use config to choose whether or not to create index again etc.

    // Firstly, we need to delete the index (we don't care if this fails)
    elasticSearchClient.execute{
      deleteIndex(elasticConfig.index)
    }.await

    // Next, we need to create the index with the correct mappings/analyzers etc.
    elasticSearchClient.execute {
      createIndex(elasticConfig.index).analysis(
        CustomAnalyzerDefinition(cBiAnalyzer, WhitespaceTokenizer, LowercaseTokenFilter)
      ).mappings(
        mapping(cBiType).fields(
          textField(cBiName).boost(cBiNameBoost).analyzer(cBiAnalyzer),
          longField(cBiUprn).analyzer(KeywordAnalyzer),
          textField(cBiPostCode).analyzer(cBiAnalyzer),
          longField(cBiIndustryCode).analyzer(KeywordAnalyzer),
          textField(cBiLegalStatus).analyzer(NotAnalyzed).includeInAll(false),
          textField(cBiTradingStatus).analyzer(NotAnalyzed).includeInAll(false),
          textField(cBiTurnover).analyzer(cBiAnalyzer),
          textField(cBiEmploymentBand).analyzer(cBiAnalyzer),
          textField(cBiPayeRefs).analyzer(KeywordAnalyzer),
          longField(cBiVatRefs).analyzer(KeywordAnalyzer),
          textField(cBiCompanyNo).analyzer(KeywordAnalyzer)
        )
      )
    }.await

    // Now we can insert some test data
    elasticSearchClient.execute {
      indexInto(elasticConfig.index / cBiType).doc(cBiName -> "Hello, world!").refresh(RefreshPolicy.IMMEDIATE)
    }

    bind(classOf[BusinessRepository]).toInstance(elasticSearchBusinessRepository)
  }
}
