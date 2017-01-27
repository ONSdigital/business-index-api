package uk.gov.ons.ingest.writers

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, LowercaseTokenFilter, StandardTokenizer}
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import scala.concurrent.Future

abstract class Initializer(
  val elastic: ElasticClient,
  val indexName: String
) {

  def mapping: Seq[MappingDefinition]

  def index: Future[CreateIndexResponse] = {
    elastic.execute {
      create.index(indexName).mappings().analysis(
        CustomAnalyzerDefinition(
          indexName + "Analyzer",
          StandardTokenizer,
          LowercaseTokenFilter
        )
      )
    }
  }
}

object Initializer {

  def apply(sources: Initializer*): Future[Seq[CreateIndexResponse]] = {
    Future.sequence(sources.map(_.index))
  }
}
