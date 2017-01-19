package uk.gov.ons.ingest.writers


import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.analyzers.{CustomAnalyzerDefinition, LowercaseTokenFilter, StandardTokenizer}
import com.sksamuel.elastic4s.mappings.MappingDefinition
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.Future

abstract class ElasticInitializer(
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

object ElasticInitializer {

  def apply[
    M[X] <: TraversableOnce[X]
  ](sources: M[ElasticInitializer]): Future[M[ElasticInitializer]] = {
    Future.sequence(sources.map(_.initialiseIndex))
  }
}
