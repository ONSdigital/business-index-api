package uk.gov.ons.bi.writers

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.AnalyzerDefinition
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.{ CreateIndexDefinition, ElasticClient }
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

import scala.concurrent.{ ExecutionContext, Future }

/**
 * A basic initializer trait that allows us to generally implement the generation
 * of multiple Elastic indexes in the same Spark application lifecycle
 * while preventing the hard-coding of specific initialisation mechanisms.
 */
trait Initializer {

  def recordName: String

  def analyzer: Option[AnalyzerDefinition] = None

  def ngramName: String = indexName + "NGramFilter"

  def analyzerName: String = indexName + "Analyzer"

  /**
   * The name of the given Elastic index.
   *
   * @return A string holding the index name. This will need to be unique and an error will occur
   *         at runtime when the application lifecycle attempts to create the index.
   */
  def indexName: String

  /**
   * Uses the Elastic4S client DSL to build a specification for a given index.
   * This will basically use a generic index construction mechanism to pre-build
   * the indexes that already exist at the time when the Spark application is executed.
   *
   * @return A mapping definition.
   */
  def indexDefinition: MappingDefinition

  protected[this] def applyAnalyzer(definition: CreateIndexDefinition): CreateIndexDefinition = {
    analyzer.fold(definition)(definition.analysis(_))
  }

  /**
   * A method that given an ElasticSearch client will trigger the creation of the index.
   * This will return a native [[org.elasticsearch.action.admin.indices.create]] that allows
   * checking whether a new index was registered or not.
   *
   * @param elastic The elastic client, passed through implicitly to avoid code duplication.
   * @return A future wrapping an index creation response.
   */
  def index()(implicit elastic: ElasticClient): Future[CreateIndexResponse] = {
    elastic.execute(applyAnalyzer(create.index(indexName).mappings(indexDefinition)))
  }
}

object Initializer {

  /**
   * Helper method to allow initialising a few indexes simultaneously using parallel writes.
   * By semantics of Future.sequence, if a single future containing an index creation will fail,
   * then the entire operation will fail.
   *
   * It's worth nothing if three futures complete and one doesn't, three indexes will be created
   * and the fourth won't, this will not implement any kind of rollback semantics for us.
   *
   * @param sources The initialise sources for Elastic indexes.
   * @param elastic The implicit Elastic client.
   * @param ctx     The implicit Scala execution context for the operations.
   * @return A future wrapping a sequence of index creation responses.
   */
  def apply(sources: Initializer*)(implicit elastic: ElasticClient, ctx: ExecutionContext): Future[Seq[CreateIndexResponse]] = {
    Future.sequence(sources.map(_.index))
  }
}