package models.db

import com.outworkers.phantom.dsl._
import play.api.libs.json.Json

import scala.concurrent.Future

case class Reviewer(
  name: String,
  phone: String,
  emailAddress: String
)

case class FeedbackEntry(
  id: UUID,
  query: String,
  specificResult: Option[String],
  comments: String,
  timestamp: DateTime
)

object FeedbackEntry {
  implicit val format = Json.format[FeedbackEntry]
}

abstract class FeedbackEntries extends CassandraTable[FeedbackEntries, FeedbackEntry] with RootConnector {
  object id extends TimeUUIDColumn(this) with PartitionKey
  object query extends StringColumn(this)
  object specificResult extends OptionalStringColumn(this)
  object comments extends StringColumn(this)
  object timestamp extends DateTimeColumn(this)

  def fromRow(row: Row): FeedbackEntry = {
    FeedbackEntry(
      id = id(row),
      query = query(row),
      specificResult = specificResult(row),
      comments = comments(row),
      timestamp = timestamp(row)
    )
  }

  def store(entry: FeedbackEntry): Future[ResultSet] = {
    insert.value(_.id, entry.id)
      .value(_.query, entry.query)
      .value(_.specificResult, entry.specificResult)
      .value(_.comments, entry.comments)
      .value(_.timestamp, entry.timestamp)
      .future()
  }

  def findById(id: UUID): Future[Option[FeedbackEntry]] = select.where(_.id eqs id).one()

  def listEntries(): Future[List[FeedbackEntry]] = select.limit(100).fetch
}

