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

object HBaseMain extends App with HBaseCache {

  def printRow(result: Result): Unit = {
    val cells = result.rawCells()
    print(Bytes.toString(result.getRow) + " : ")
    for (cell <- cells) {
      val col_name = Bytes.toString(CellUtil.cloneQualifier(cell))
      val col_value = Bytes.toString(CellUtil.cloneValue(cell))
      print("(%s,%s) ".format(col_name, col_value))
    }
    println()
  }


  def tableName = "test_t2"


  // Put example
  var put = new Put("row1")
  put.addColumn("d", "test_column_name", "test_value")
  put.addColumn("d", "test_column_name2", "test_value2")
  table.put(put)

  // Get example
  println("Get Example:")
  var get = new Get("row1")
  var result = table.get(get)
  printRow(result)

  //Scan example
  println("\nScan Example:")
  var scan = table.getScanner(new Scan())
  scan.asScala.foreach(result => {
    printRow(result)
  })

  table.close()
  connection.close()

  override def config: Config = BiConfigManager.envConf(ConfigFactory.load())
}