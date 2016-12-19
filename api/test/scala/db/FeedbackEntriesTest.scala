package scala.db

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import com.outworkers.util.testing._

class FeedbackEntriesTest extends FlatSpec with Matchers with OptionValues with ScalaFutures {

  it should "store and retrieve a simple feedback entry from the database" in {
    val entry = gen[FeedbackEntry]
  }

}
