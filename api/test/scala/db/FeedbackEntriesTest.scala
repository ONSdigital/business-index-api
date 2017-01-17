package scala.db

import com.datastax.driver.core.utils.UUIDs
import com.outworkers.util.testing._

class FeedbackEntriesTest extends DatabaseSuite {

  it should "store and retrieve a simple feedback entry from the database" in {
    val entry = gen[FeedbackEntry].copy(id = UUIDs.timeBased())

    val chain = for {
      _ <- database.feedbackEntries.store(entry)
      record <- database.feedbackEntries.findById(entry.id)
    } yield record

    whenReady(chain) { res =>
      res shouldBe defined
      res.value shouldEqual entry
    }
  }

}
