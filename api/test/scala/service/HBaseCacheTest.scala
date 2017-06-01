package scala.service

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import services.HBaseCache
import uk.gov.ons.bi.writers.BiConfigManager

/**
  * Created by Volodymyr.Glushak on 05/05/2017.
  */
class HBaseCacheTest extends FlatSpec with Matchers with HBaseCache with BeforeAndAfterAll {
  // set up hbase using internal memeory - mimic
  private[this] val utility = HBaseTesting.hBaseServer

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

  override protected def tableName: String = config.getString("hbase.requests.table.name")
}
