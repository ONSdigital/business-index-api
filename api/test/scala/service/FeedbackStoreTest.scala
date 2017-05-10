package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import controllers.v1.feedback.FeedbackObj
import org.apache.hadoop.conf.Configuration
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.store.FeedbackStore
import uk.gov.ons.bi.writers.BiConfigManager

class FeedbackStoreTest extends FlatSpec with Matchers with FeedbackStore with BeforeAndAfterAll {

  private[this] val utility = HBaseTesting.hBaseServer

  override protected val conf: Configuration = utility.getHBaseAdmin.getConfiguration
  val before = utility.countRows(table)
  val record1 = FeedbackObj(None, "doej", "John Doe", "01/01/2000", "UI Issue", Some(List(898989898989L)), Some("BusinessName:test&limit=100"), "UBRN does not match given company name.")
  val record2 = FeedbackObj(None, "coolit", "Tom Colling", "03/11/2011", "UI Issue", Some(List(117485788989L)), None, "UBRN does match for test company.")

  "It" should "accept a valid feedbackObj" in {
    val expected = "doej01/01/2000"
    val feedback = store(record1)
    feedback shouldBe expected
    utility.countRows(table) shouldBe before + 1
  }

  "It" should "show all (2) records in hbase" in {
    store(record1)
    store(record2)
    utility.countRows(table) shouldBe before + 2
    getAll(false).length shouldBe before + 2
  }

  "It" should "only display records with hide status false" in {
    val key = "doej01/01/2000"
    store(record1)
    store(record2)
//    hide(key)

    utility.countRows(table) shouldBe before + 2
    getAll(false).length shouldBe before + 2
    val objList = getAll(false)

    objList.foreach {
      x => x.hideStatus shouldBe Some(false)
    }
  }


  override def afterAll(): Unit = {
    super.afterAll()
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = config.getString("hbase.feedback.table.name")
}


