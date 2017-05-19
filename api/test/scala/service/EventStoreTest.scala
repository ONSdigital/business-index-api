package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.store.{DeleteCommand, EventCommand, EventStore}
import uk.gov.ons.bi.models.BusinessIndexRec
import uk.gov.ons.bi.writers.BiConfigManager

class EventStoreTest extends FlatSpec with Matchers with EventStore with BeforeAndAfterAll {

  private[this] val utility = HBaseTesting.hBaseServer

  "It" should "store events properly" in {
    val bir = BusinessIndexRec(90001, "EventStoreName", None, None, None, None, None, None, None, None, None, None)

    val instructions = EventCommand(bir, DeleteCommand)

    (0 to 10).foreach(x => storeEvent(instructions.copy(event = bir.copy(id = 1L + x))))
    val data = getAll.filter(_.event.businessName == "EventStoreName")

    data.size shouldBe 11

    var i = 0L
    data.map(_.event.id).foreach(id => {
      id shouldBe >(i)
      i = id
    })


    cleanAll()
    storeEvent(instructions.copy(event = bir.copy(businessName = "EventStoreName2")))
    getAll.count(_.event.businessName == "EventStoreName2") shouldBe 1
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = config.getString("hbase.events.table.name")

}
