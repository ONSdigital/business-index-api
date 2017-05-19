package services.store

import controllers.v1.feedback.FeedbackObj
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.client.{Delete, Put, Scan}
import org.apache.hadoop.hbase.filter.{CompareFilter, SingleColumnValueFilter}
import services.HBaseCore

import scala.collection.JavaConverters._


/**
  * Created by haqa on 08/05/2017.
  */
trait FeedbackStore extends HBaseCore {

  protected val columnFamily = "feedback"

  protected def store(feedback: FeedbackObj): String = {
    val id = feedback.username + feedback.date.getOrElse(System.currentTimeMillis()).toString
    logger.debug(s"A new record with id $id has been added to the HBase table feedback_tbl")
    val put = new Put(id)
    // same each element of feedbackobject as a colum in tbl
    FeedbackObj.toMap(feedback, id).foreach { case (k, v) =>
      put.addColumn(columnFamily, k, v.toString)
    }
    table.put(put)
    id
  }

  protected def getAll(hidden: Boolean = false): List[FeedbackObj] = {
    val s = new Scan()
    if (!hidden) {
      s.setFilter(new SingleColumnValueFilter(columnFamily, "hideStatus", CompareFilter.CompareOp.EQUAL, "false"))
    }
    val scan = table.getScanner(s)
    val res = scan.asScala.map { res =>
      val map = res.listCells().asScala.map { cell =>
        asString(CellUtil.cloneQualifier(cell)) -> asString(CellUtil.cloneValue(cell))
      }.toMap
      FeedbackObj.fromMap(map)
    }.toList
    res
  }

  protected def getById(id: String): Option[FeedbackObj] = {
    val s = new Scan()
    s.setFilter(new SingleColumnValueFilter(columnFamily, "id", CompareFilter.CompareOp.EQUAL, id))
    val scan = table.getScanner(s)
    scan.asScala.map { res =>
      val map = res.listCells().asScala.map { cell =>
        asString(CellUtil.cloneQualifier(cell)) -> asString(CellUtil.cloneValue(cell))
      }.toMap
      FeedbackObj.fromMap(map)
    }.toList.headOption
  }

  protected def hide(id: String, status: Option[Boolean] = Some(true)): String = {
    getById(id).getOrElse(sys.error(s"Id $id is not found in database - can't hide."))
    val put = new Put(id)
    put.addColumn(columnFamily, "hideStatus", status.getOrElse("true").toString)
    table.put(put)
    logger.debug(s"The data row $id has been set to hide.")
    id
  }

  protected def delete(id: String): String = {
    getById(id).getOrElse(sys.error(s"Id $id is not found in database - can't hide."))
    table.delete(new Delete(id))
    logger.debug(s"The record with id $id has been deleted from HBase")
    id
  }

  protected def progress(feedbackObj: FeedbackObj): FeedbackObj = {
    val id = feedbackObj.id.getOrElse(sys.error("No id provided"))
    val oldFeedbackObj = getById(id).getOrElse(sys.error(s"Id $id is not found in database - can't update."))
    val put = new Put(id)
    val newStatus = feedbackObj.progressStatus.getOrElse("New").toString
    put.addColumn(columnFamily, "progressStatus", newStatus)
    table.put(put)
    logger.debug(s"The data row ${feedbackObj.id} has been set with a Progress Status of $newStatus.")
    oldFeedbackObj.copy(progressStatus = Some(newStatus))
  }

}
