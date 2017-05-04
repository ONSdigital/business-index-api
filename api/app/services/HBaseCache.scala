package services

import com.typesafe.config.Config
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.apache.hadoop.security.UserGroupInformation
import org.slf4j.LoggerFactory

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

  config.getString("hbase.props").split(",").map(_.trim).foreach { pr =>
    conf.set(pr, config.getString(pr))
  }

  // everything lazy: initialized only when used...
  protected lazy val connection: Connection = {
    val username = config.getString("hbase.login")
    val keytab = config.getString("hbase.keytab.path")
    logger.info(s"Starting kerberos authentication with $username and path to key: $keytab")
    UserGroupInformation.setConfiguration(conf)
    UserGroupInformation.loginUserFromKeytab(username, keytab)
    ConnectionFactory.createConnection(conf)
  }

  // kerberos ticket will expire in 24h of application running - we want to make sure we've got new ticket when required
  private[this] def refreshTicket() = UserGroupInformation.getLoginUser.checkTGTAndReloginFromKeytab()

  protected lazy val table: Table = connection.getTable(TableName.valueOf(tableName))

  protected def getFromCache(request: String): Option[String] = {
    refreshTicket()
    logger.debug(s"Requesting value from cache for $request")
    val result = table.get(new Get(request))
    if (result.isEmpty) None else {
      logger.debug(s"Got value from cache for $request")
      result.rawCells().map(cell => Bytes.toString(CellUtil.cloneValue(cell))).headOption
    }
  }

  protected def updateCache(request: String, newValue: String): String = {
    val put = new Put(request)
    put.addColumn("d", "response", newValue)
    table.put(put)
    newValue
  }

}