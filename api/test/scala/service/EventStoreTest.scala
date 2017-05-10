package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.store.EventStore
import uk.gov.ons.bi.writers.BiConfigManager

class EventStoreTest extends FlatSpec with Matchers with EventStore with BeforeAndAfterAll {

  private[this] val utility = HBaseTesting.hBaseServer

  "It" should "store events properly" in {
    val instructions = "{command: DELETE, id: 1}"
    (0 to 10).foreach { x =>
      storeEvent(instructions)
    }
    getAll.count { case (_, v) => v == instructions }  shouldBe 11
    val firstTime = getAll.headOption.map { case (k, _) => k.toLong }.getOrElse(sys.error("No data in HBase"))
    getAll.filter { case (_, v) => v == instructions } .tail.foreach { case (rc, _) =>
      rc.toLong shouldBe >(firstTime)
    }
    cleanAll()
    storeEvent(instructions)
    getAll.count { case (_, v) => v == instructions } shouldBe  1
  }

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())

  override protected def tableName: String = config.getString("hbase.events.table.name")
}
