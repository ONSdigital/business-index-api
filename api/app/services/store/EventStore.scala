package services.store

import java.util

import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Delete, Put, Scan}
import services.HBaseCore
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.collection.JavaConverters._
import controllers.v1.BusinessIndexObj._

/**
  * Created by VG on 09/05/2017.
  */
trait EventStore extends HBaseCore {

  protected val columnFamily = "event"

  private[this] val jsonColumn = "json"
  private[this] val instructionColumn = "instruction"


  def storeEvent(eventCommand: EventCommand): String = {
    val id = System.currentTimeMillis().toString
    val put = new Put(id)
    put.addColumn(columnFamily, jsonColumn, biToJson(eventCommand.event).toString())
    put.addColumn(columnFamily, instructionColumn, eventCommand.command.toString)
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
  def getAll: List[EventCommand] =
    table.getScanner(new Scan()).asScala.map { res =>
      val colsMap = res.listCells().asScala.map { cell =>
        CellUtil.cloneQualifier(cell).asString() -> CellUtil.cloneValue(cell).asString()
      }.toMap
      res.getRow.asString -> EventCommand(biFromJson(colsMap(jsonColumn)), Command.fromString(colsMap(instructionColumn)))

      }.toList.sortBy { case (k, _) => k }.map { case (_, v) => v }
}


sealed trait Command

case object StoreCommand extends Command

case object DeleteCommand extends Command

object Command {

  def fromString(s: String): Command = {
    s match {
      case "StoreCommand" => StoreCommand
      case "DeleteCommand" => DeleteCommand
    }
  }

}

case class EventCommand(event: BusinessIndexRec, command: Command)