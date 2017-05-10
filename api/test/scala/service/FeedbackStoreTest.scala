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
  private[this] val before = utility.countRows(table)

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
    val objList = getAll()
    objList.length shouldBe >= (before + 2)
    objList.toString should include ("doej01:01:2000")
    objList.toString should include ("drake03:11:2011")
  }

  "It" should "only display records with hide status false" in {
    val key = "doej01:01:2000"
    store(recordObj(username = "kali", name = "kali", date = "13:11:2015", ubrn = Some(List(896767288989L)), query = None))
    store(recordObj(username = "sungj", name = "Jim Sung", date = "03:11:2011", ubrn = Some(List(117485788989L)), query = None))
    hide(key)

    utility.countRows(table) shouldBe >= (before + 1)

    val objList = getAll()
    objList.length shouldBe >= (before + 1)

    val ids = List("kali13:11:2015", "sungj03:11:2011", "drake03:11:2011" )
    println("vlod: " + objList)
    objList.foreach { x =>
      ids.count( idd => x.id.contains(idd)) shouldBe 1
      x.hideStatus shouldBe Some(false)
    }
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = config.getString("hbase.feedback.table.name")
}


