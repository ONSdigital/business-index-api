package services

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{CellUtil, HBaseConfiguration, TableName}
import org.apache.hadoop.security.UserGroupInformation
import uk.gov.ons.bi.Utils

trait HBaseCache extends HBaseCore {
  protected val columnFamily = "d"

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

trait HBaseImplicitUtils {
  // All HBase API rely on this function: it expect byte[] everywhere, instead of Strings.
  implicit protected def asBytes(s: String): Array[Byte] = Bytes.toBytes(s)



  implicit class BytesArr(b: Array[Byte]) {
    def asString(): String = Bytes.toString(b)
  }
}

trait HBaseCore extends StrictLogging with HBaseImplicitUtils {

  def config: Config

  protected def tableName: String

  protected def asString(bytes: Array[Byte]): String = Bytes.toString(bytes)

  // it's a method: assume to use only on initialization stage
  // overriden for tests
  protected def conf: Configuration = {
    val c = HBaseConfiguration.create()
    config.getString("hbase.props").split(",").map(_.trim).foreach { pr =>
      val v = Utils.configOverride(pr)(config)
      logger.debug(s"Setup prop $pr with value $v")
      c.set(pr, v)
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
  protected[this] def refreshTicket(): Unit = if (secureConnection) UserGroupInformation.getLoginUser.checkTGTAndReloginFromKeytab()

  protected lazy val table: Table = connection.getTable(TableName.valueOf(tableName))
}
