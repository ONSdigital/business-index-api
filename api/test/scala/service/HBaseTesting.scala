package scala.service

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.hbase.HBaseTestingUtility
import services.HBaseImplicitUtils
import uk.gov.ons.bi.writers.BiConfigManager

/**
  * Created by Volodymyr.Glushak on 09/05/2017.
  */
object HBaseTesting extends HBaseImplicitUtils {

  val hBaseServer: HBaseTestingUtility = {
    val u = new HBaseTestingUtility()
    u.getConfiguration.setBoolean("fs.hdfs.impl.disable.cache", true)
    u.startMiniCluster
    val clientPortProp = "hbase.zookeeper.property.clientPort"
    System.setProperty(clientPortProp, u.getConfiguration.get(clientPortProp))
    // create all necessary tables in our HBase
    val config = BiConfigManager.envConf(ConfigFactory.load())
    u.createTable(config.getString("hbase.requests.table.name"), "d")
    u.createTable(config.getString("hbase.events.table.name"), "event")
    u.createTable(config.getString("hbase.feedback.table.name"), "feedback")
    u
  }
}
