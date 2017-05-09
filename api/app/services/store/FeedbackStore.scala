package services.store

import controllers.v1.feedback.FeedbackObj
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Delete, Put, Scan}
import org.apache.hadoop.hbase.util.Bytes
import services.HBaseCore

import scala.collection.JavaConverters._


/**
  * Created by haqa on 08/05/2017.
  */
trait FeedbackStore extends HBaseCore {

  protected val columnFamily = "feedback"

  protected def store(feedback: FeedbackObj): String = {
    val id = feedback.username + feedback.date
    logger.debug(s"A new record with id ${id} has been added to the HBase table feedback_tbl")
    val put = new Put(id)
    // same each element of feedbackobject as a colum in tbl
    FeedbackObj.toMap(feedback, id).foreach { case (k, v) =>
      put.addColumn(columnFamily, k, v.toString)
    }
    table.put(put)
    id
  }

  protected def delete(id : String) = {
    table.delete(new Delete(id))
    logger.debug(s"The record with id $id has been deleted from HBase table feedback_tbl")
    id
  }

  protected def getAll(statusChecker: Boolean): List[FeedbackObj] = {
    val scan = table.getScanner(new Scan())

    val res = scan.asScala.map { res =>
      val map = res.listCells().asScala.map { cell =>
        Bytes.toString(CellUtil.cloneQualifier(cell)) -> Bytes.toString(CellUtil.cloneValue(cell))
      }.toMap
      FeedbackObj.fromMap(map)

    }.toList
    res
  }

  protected def hide (id: String, status: Option[Boolean] = Some(true)) = {
    val put = new Put (id)
    put.addColumn(columnFamily, "hideStatus", status.toString )
    table.put(put)
    logger.debug(s"The data row ${id} has been set to hide.")
  }



}
