package services

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.slf4j.LoggerFactory
import uk.gov.ons.bi.writers.BiConfigManager

import scala.collection.JavaConverters._

trait HBaseCache {

  // All HBase API rely on this function: it expect byte[] everywhere, instead of Strings.
  implicit def asBytes(s: String): Array[Byte] = Bytes.toBytes(s)
  
  private[this] val logger = LoggerFactory.getLogger(getClass)

  def config: Config

  protected def tableName: String

  private[this] val conf = HBaseConfiguration.create()

  /**
    * From http://hbase.apache.org/0.94/book/zookeeper.html
    * A distributed Apache HBase (TM) installation depends on a running ZooKeeper cluster. All participating nodes and clients
    * need to be able to access the running ZooKeeper ensemble. Apache HBase by default manages a ZooKeeper "cluster" for you.
    * It will start and stop the ZooKeeper ensemble as part of the HBase start/stop process. You can also manage the ZooKeeper
    * ensemble independent of HBase and just point HBase at the cluster it should use. To toggle HBase management of ZooKeeper,
    * use the HBASE_MANAGES_ZK variable in conf/hbase-env.sh. This variable, which defaults to true, tells HBase whether to
    * start/stop the ZooKeeper ensemble servers as part of HBase start/stop.
    */
  conf.set("hbase.zookeeper.quorum", config.getString("hbase.zookeeper.quorum"))

  // everything lazy: initialized only when used...
  protected lazy val connection: Connection = ConnectionFactory.createConnection(conf)

  protected lazy val table: Table = connection.getTable(TableName.valueOf(tableName))

  protected def getOrElseUpdateCache(request: String)(f: => String): String = {
    val result = table.get(new Get(request))
    if (result.isEmpty) {
      val propString = f
      val put = new Put(request)
      put.addColumn("d", "response", propString)
      table.put(put)
      propString
    } else {
      logger.debug(s"Value from cache for $request")
      result.rawCells().map(cell => Bytes.toString(CellUtil.cloneValue(cell))).head
    }
  }

}