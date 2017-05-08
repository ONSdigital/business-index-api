package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hbase.HBaseTestingUtility
import org.apache.hadoop.hbase.zookeeper.TestZooKeeperMainServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.HBaseCache
import uk.gov.ons.bi.writers.BiConfigManager

/**
  * Created by Volodymyr.Glushak on 05/05/2017.
  */
class HBaseCacheTest extends FlatSpec with Matchers with HBaseCache with BeforeAndAfterAll {

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

    val key = "BusinessName:AAA"
    val value = "{ empty }"
    updateCache(key, value)
    getFromCache(key) shouldBe Some(value)
    utility.countRows(table) shouldBe before + 1
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = "es_requests"
}
