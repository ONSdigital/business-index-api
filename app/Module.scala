import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.http.ElasticDsl
import play.api.{ Configuration, Environment }
import services.BusinessService
import repository.ElasticSearchBusinessRepository
import utils.{ ElasticClient, ElasticRequestMapper }

import config.ElasticSearchConfigLoader

class Module(environment: Environment, configuration: Configuration) extends AbstractModule with ElasticDsl {

  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val elasticConfig = ElasticSearchConfigLoader.load(underlyingConfig)
    val elasticSearchClient = ElasticClient.getElasticClient(elasticConfig)
    val elasticSearchBusinessRepository = new ElasticSearchBusinessRepository(elasticSearchClient, new ElasticRequestMapper, elasticConfig)
    bind(classOf[BusinessService]).toInstance(elasticSearchBusinessRepository)
  }
}
