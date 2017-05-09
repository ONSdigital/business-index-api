package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import controllers.v1.FeedbackObj
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseTestingUtility
import org.apache.hadoop.hbase.zookeeper.TestZooKeeperMainServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.store.FeedbackStore
import uk.gov.ons.bi.writers.BiConfigManager

class FeedbackStoreTest extends FlatSpec with Matchers with FeedbackStore with BeforeAndAfterAll {

  //  val confg = new Configuration()
  //  confg.setBoolean
  //  val fileSystem: FileSystem = FileSystem.get(confg)

  val zooServer = new TestZooKeeperMainServer
  private[this] val utility = new HBaseTestingUtility()
  utility.getConfiguration.setBoolean("fs.hdfs.impl.disable.cache", true)
  utility.startMiniCluster
  utility.createTable(tableName, columnFamily)

  override protected val conf: Configuration = utility.getHBaseAdmin.getConfiguration

  "It" should "cache values properly" in {
    val before = utility.countRows(table)
    val key = store(FeedbackObj("doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), None, "UBRN does not match given company name."))
    store(FeedbackObj("coolit", "Tom Colling", "03/11/2011", "UI Issue", Some(List(117485788989L)), None, "UBRN does match for test company."))
    utility.countRows(table) shouldBe before + 2
    println(getAll())
    getAll().length shouldBe before + 2
    delete(key)
    getAll().length shouldBe before + 1
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = "feedback_tbl"
}
