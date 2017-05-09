package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import controllers.v1.feedback.FeedbackObj
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseTestingUtility
import org.apache.hadoop.hbase.zookeeper.TestZooKeeperMainServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.test.Helpers.{contentAsString, contentType, status}
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
  val before = utility.countRows(table)

//  "It" should "cache values properly" in {
//    val expected = ""
//    val feedback = store(FeedbackObj(None, "doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), Some("BusinessName:test&limit=100"), "UBRN does not match given company name."))
//    println ("feedback: " + feedback)
////    shouldBe Some(expected)
//  }


  "It" should "cache values properly" in {

    store(FeedbackObj(None, "doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), Some("BusinessName:test&limit=100"), "UBRN does not match given company name."))
    println("GetAll() ====90000000" + getAll(false))
    val key = "doej01/01/2000"

    store(FeedbackObj(None, "coolit", "Tom Colling", "03/11/2011", "UI Issue", Some(List(117485788989L)), None, "UBRN does match for test company."))
    utility.countRows(table) shouldBe before + 2
    println("GetAll()" + getAll(false))
    getAll(false).length shouldBe before + 2
    delete(key)
    println("GetAll() -- 2" + getAll(false))
//    println("hide: ===" + hide("coolit03/11/2011"))
//    getAll(false).length shouldBe before + 1
//    store(FeedbackObj(None,"doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), Some("BusinessName:test&limit=100"), "UBRN does not match given company name."))
//    println("get everything here === T" + getAll(false))
//    println("GET ONLY THOSE WITH HIDING STATUS - TRuE === T" + getAll(true))
  }


  /**
    * 1. store record normally
    * 2. store with missing field in different type format
    * 3. submit null with store
    * 4. delete - hide with true
    * 5. display all - add two reecord and show / delete one and show
    * 6. override id or hide status
    */

  override def afterAll(): Unit = {
    super.afterAll()
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = "feedback_tbl"
}
