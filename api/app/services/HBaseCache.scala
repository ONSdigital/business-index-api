package services

import com.typesafe.config.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.apache.hadoop.security.UserGroupInformation
import org.slf4j.LoggerFactory

trait HBaseCache {

  // All HBase API rely on this function: it expect byte[] everywhere, instead of Strings.
  implicit protected def asBytes(s: String): Array[Byte] = Bytes.toBytes(s)

  private[this] val logger = LoggerFactory.getLogger(getClass)

  def config: Config

  protected def tableName: String
  protected val columnFamily = "d"

  // it's a method: assume to use only on initialization stage
  // overriden for tests
  protected def conf: Configuration = {
    val c = HBaseConfiguration.create()
    config.getString("hbase.props").split(",").map(_.trim).foreach { pr =>
      c.set(pr, config.getString(pr))
    }
    c
  }

  private[this] val secureConnection: Boolean = config.getBoolean("hbase.kerberos.enabled")

  // everything lazy: initialized only when used...
  protected lazy val connection: Connection = {
    if (secureConnection) {
      val username = config.getString("hbase.login")
      val keytab = config.getString("hbase.keytab.path")
      logger.info(s"Starting kerberos authentication with $username and path to key: $keytab")
      UserGroupInformation.setConfiguration(conf)
      UserGroupInformation.loginUserFromKeytab(username, keytab)
    }
    ConnectionFactory.createConnection(conf)
  }

  // kerberos ticket will expire in 24h of application running - we want to make sure we've got new ticket when required
  private[this] def refreshTicket() = if (secureConnection) UserGroupInformation.getLoginUser.checkTGTAndReloginFromKeytab()

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
    put.addColumn(columnFamily, "response", newValue)
    table.put(put)
    newValue
  }

}