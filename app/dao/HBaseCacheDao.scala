package dao

import javax.inject.Inject

import connector.HBaseConnector
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

//@Singleton
class HBaseCacheDao @Inject() (connector: HBaseConnector)(implicit context: ExecutionContext) {

  val tableName = "es_requests"

  val logger = LoggerFactory.getLogger(getClass)

  private def closeTable(table: Table) = Try(table.close).getOrElse(
    logger.warn(s"Could not close HBase Table: ${table.getName}. Continuing anyway...")
  )

  implicit def asBytes(s: String): Array[Byte] = Bytes.toBytes(s)

  def getFromCache(request: String): Future[Option[String]] = Future {
    val table: Table = connector.getTable(tableName)
    val result = table.get(new Get(request))

    if (result.isEmpty) None else {
      logger.debug(s"Value from cache for $request")
      logger.debug("closing Table....")
      closeTable(table)
      result.rawCells().map(cell => Bytes.toString(CellUtil.cloneValue(cell))).headOption
    }

  }

  def updateCache(request: String, newValue: String): Future[String] = Future {
    val table: Table = connector.getTable(tableName)
    val put = new Put(request)
    put.addColumn("d", "response", newValue)
    table.put(put)
    logger.debug("closing Table....")
    closeTable(table)
    newValue
  }
}

