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
  
  def recordObj (id: Option[String] = None, username: String = "doej", name: String = "John Doe", date: String = "01:01:2000", subject: String = "Data Issue", ubrn: Option[List[Long]] = Some(List(898989898989L, 111189898989L)), query: Option[String] = Some("BusinessName:test&limit=100"), comments: String = "UBRN does not match given company name.", hideStatus: Option[Boolean] = Some(false)) = FeedbackObj(id, username, name, date, subject, ubrn, query, comments, hideStatus )


  "It" should "accept a valid feedbackObj" in {
    val expected = "doej01:01:2000"
    val feedback = store(recordObj())
    feedback shouldBe expected
    utility.countRows(table) shouldBe > (before)

  }

  "It" should "show all (2) records in hbase" in {
    store(recordObj())
    store(recordObj(username = "drake", name = "drake", date = "03:11:2011", ubrn = Some(List(117485788989L)), query = None))
    utility.countRows(table) shouldBe >= (before + 2)
    val objList = getAll(false)
    objList.length shouldBe >= (before + 2)
    objList.toString should include ("doej01:01:2000")
    objList.toString should include ("drake03:11:2011")
    // check to see if it has a specific reecord by an id for each two case rather than just count
  }

  "It" should "only display records with hide status false" in {
    val key = "doej01:01:2000"
    store(recordObj(username = "kali", name = "kali", date = "13:11:2015", ubrn = Some(List(896767288989L)), query = None))
    store(recordObj(username = "sungj", name = "Jim Sung", date = "03:11:2011", ubrn = Some(List(117485788989L)), query = None))
//    hide(key)

    utility.countRows(table) shouldBe >= (before + 2)
    val objList = getAll(false)
    objList.length shouldBe >= (before + 2)

    objList.toString should include ("kali13:11:2015")
    objList.toString should include ("sungj03:11:2011")

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


