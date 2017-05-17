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

  def recordObj (id : Option [String] = None, username: String = "duportj", name: String = "Juan Duport", date: Option[String] = Some("01:01:2000"), subject: String = "Data Issue", ubrn: Option[List[Long]] = Some(List(898989898989L, 111189898989L)), query: Option[String] = Some("BusinessName:test&limit=100"), progressStatus: Option[String] = Some("New")) = FeedbackObj(id, username, name, date, subject, ubrn, query, "UBRN does not match given company name.", progressStatus )

  "It" should "accept a valid feedbackObj" in {
    val expected = "duportj01:01:2000"
    val feedback = store(recordObj())
    feedback shouldBe expected
    utility.countRows(table) shouldBe >= (before)
  }

  "It" should "show all (2) records in hbase" in {
    store(recordObj())
    store(recordObj(username = "cruzj", name = "Juan dela Cruz", date = Some("03:11:2011"), ubrn = Some(List(117485788989L)), query = None))
    val objList = getAll()
    val ids = List("duportj01:01:2000", "cruzj03:11:2011")
    objList.foreach { x =>
      ids.count(i => x.id.contains(i))
      x.hideStatus shouldBe Some(false)
    }

  }

  "It" should "only display records with hide status false" in {
    val delete = List("duportj01:01:2000", "cruzj03:11:2011")
    delete.foreach { x =>  hide(x)}
    store(recordObj(username = "bloggsj", name = "Joe Bloggs", date = Some("13:11:2015"), ubrn = Some(List(896767288989L)), query = None))
    store(recordObj(username = "sanz", name = "Zhang San", date = Some("03:11:2011"), ubrn = Some(List(117485788989L)), query = None))


    utility.countRows(table) shouldBe >=(before)

    val objList = getAll()
    objList.length shouldBe >=(before + 1)

    val ids = List("bloggsj13:11:2015", "sanz03:11:2011")
    objList.foreach { x =>
      ids.count(idd => x.id.contains(idd))
      x.hideStatus shouldBe Some(false)
    }

  }

  "It" should "update the progress status of a given id" in {
    val idd = store(recordObj(username = "mustermannm", name = "Max Mustermann", progressStatus = Some("Completed")))
    utility.countRows(table) shouldBe >= (before + 1)
    val change = progress(recordObj(id = Some(idd), progressStatus = Some("In Progress") ))
    change.progressStatus == "In Progress"
  }


  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = config.getString("hbase.feedback.table.name")
}


