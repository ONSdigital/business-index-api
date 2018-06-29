import com.google.inject.AbstractModule
import play.api.{ Configuration, Environment }
import services.BusinessRepository
import repository.ElasticSearchBusinessRepository
import utils.{ ElasticClient, ElasticResponseMapper, ElasticResponseMapperSecured, ElasticUtils }
import config.ElasticSearchConfigLoader

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val elasticConfig = ElasticSearchConfigLoader.load(underlyingConfig)
    val elasticSearchClient = ElasticClient.getElasticClient(elasticConfig)
    val esUtils = new ElasticUtils(elasticSearchClient, elasticConfig)
    val elasticSearchBusinessRepository = new ElasticSearchBusinessRepository(
      elasticSearchClient, new ElasticResponseMapper, new ElasticResponseMapperSecured, elasticConfig
    )

    if (elasticConfig.recreateIndex) esUtils.recreateIndex()
    if (elasticConfig.loadTestData) esUtils.insertTestData()

    bind(classOf[BusinessRepository]).toInstance(elasticSearchBusinessRepository)
  }
}
