package scala.db

import com.outworkers.util.testing._

class FeedbackEntriesTest extends DatabaseSuite {

  it should "store and retrieve a simple feedback entry from the database" in {
    val entry = gen[FeedbackEntry]
  }

}
