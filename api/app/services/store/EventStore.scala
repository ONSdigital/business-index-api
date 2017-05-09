package services.store

import java.util

import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Delete, Put, Scan}
import services.HBaseCore

import scala.collection.JavaConverters._


/**
  * Created by VG on 09/05/2017.
  */
trait EventStore extends HBaseCore {

  protected val columnFamily = "event"

  def storeEvent(event: String): String = {
    val id = System.currentTimeMillis().toString
    val put = new Put(id)
    put.addColumn(columnFamily, "json", event)
    table.put(put)
    id
  }

  def cleanAll(): Unit = {
    val list = table.getScanner(new Scan()).iterator().asScala.map { rr =>
      new Delete(rr.getRow)
    }
    table.delete(new util.ArrayList[Delete](list.toList.asJava))
  }

  // return list of all events ordered by time
  def getAll: List[(String, String)] =
    table.getScanner(new Scan()).asScala.flatMap { res =>
      val json = res.listCells().asScala.map { cell =>
        CellUtil.cloneValue(cell).asString()
      }.headOption
      json.map(jv => res.getRow.asString -> jv)
    }.toList.sortBy { case (key, _) => key }
}
