package uk.gov.ons.bi.writers

//import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
//import com.sksamuel.elastic4s.{ BulkResult, ElasticClient, ElasticsearchClientUri }
import com.typesafe.config.Config
//import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
//import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import uk.gov.ons.bi.Utils._
import uk.gov.ons.bi.{ DataSource, MapDataSource }
import uk.gov.ons.bi.models.BIndexConsts._
import uk.gov.ons.bi.models.BusinessIndexRec
//import uk.gov.ons.bi.writers.indexes.BusinessIndex

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
 * Created by Volodymyr.Glushak on 10/02/2017.
 */
//class ElasticImporter()(implicit val config: Config, elastic: ElasticClient) {
//
//  private[this] val logger = LoggerFactory.getLogger(getClass)
//
//  val BatchSize: Int = getPropOrElse("elastic.importer.batch.size", "1000").toInt
//  val TheadDelays: Int = getPropOrElse("elastic.importer.delay.ms", "5").toInt
//
//  def initializeIndex(businessIndex: String): Future[CreateIndexResponse] = {
//    Try(elastic.execute {
//      delete index businessIndex
//    }) // ignore if index doesn't exists
//    Initializer(BusinessIndex(businessIndex)).map(_.head)
//  }
//
//  def loadBusinessIndexFromMapDS(indexName: String, d: DataSource[String, BusinessIndexRec]): Future[Iterator[BulkResult]] = d match {
//    case data: MapDataSource[String, BusinessIndexRec] => loadBusinessIndex(indexName, data.data.values.toSeq)
//  }
//
//  def loadBusinessIndex(indexName: String, d: Seq[BusinessIndexRec]): Future[Iterator[BulkResult]] = {
//    val r = d.grouped(BatchSize).map { biMap =>
//      if (TheadDelays > 0) Thread.sleep(TheadDelays.toLong)
//      logger.debug(s"Bulk of size ${biMap.size} is about to be processed...")
//      elastic.execute {
//        bulk(
//          biMap.map { bi =>
//            logger.trace(s"Indexing entry in ElasticSearch $bi")
//            index into indexName / cBiType id bi.id fields BusinessIndexRec.toMap(bi)
//          }
//        )
//      }
//    }
//    Future.sequence(r)
//  }
//}

case class ElasticConfiguration(localMode: Boolean, clusterName: String, uri: String, sniffEnabled: Boolean)

object BiConfigManager {

  private[this] val logger = LoggerFactory.getLogger(BiConfigManager.getClass)

  def envConf(conf: Config): Config = {
    val env = sys.props.get("environment").getOrElse("default")
    logger.info(s"Load config for [$env] env")
    val envConf = conf.getConfig(s"env.$env")
    logger.debug(envConf.toString)
    envConf
  }
}

//object ElasticClientBuilder {
//
//  def build(envConf: Config): HttpClient = {
//    val cfg = ElasticConfiguration(
//      localMode = envConf.getBoolean("elasticsearch.local"),
//      clusterName = envConf.getString("elasticsearch.cluster.name"),
//      uri = envConf.getString("elasticsearch.uri"),
//      sniffEnabled = envConf.getBoolean("elasticsearch.client.transport.sniff")
//    )
//    buildWithConfig(cfg)
//  }
//
//  def buildWithConfig(config: ElasticConfiguration): HttpClient = {
//    val settings = Settings.settingsBuilder().put("client.transport.sniff", config.sniffEnabled)
//
//    if (config.localMode) {
//      ElasticClient.local(settings.put("path.home", System.getProperty("java.io.tmpdir")).build())
//    } else {
//      ElasticClient.transport(settings.put("cluster.name", config.clusterName).build(), ElasticsearchClientUri(config.uri))
//    }
//    HttpClient.
//  }
//}
