package models.db

import java.util.concurrent.TimeUnit

import com.outworkers.phantom.dsl.{DateTime, UUID}
import com.outworkers.util.testing.Sample
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import com.outworkers.util.testing._

trait DatabaseSuite extends FlatSpec with Matchers with OptionValues with ScalaFutures with TestDbProvider {

  protected[this] val defaultScalaTimeoutSeconds = 25

  private[this] val defaultScalaInterval = 50L

  implicit val defaultScalaTimeout = scala.concurrent.duration.Duration(defaultScalaTimeoutSeconds, TimeUnit.SECONDS)

  private[this] val defaultTimeoutSpan = Span(defaultScalaTimeoutSeconds, Seconds)

  implicit val defaultTimeout: PatienceConfiguration.Timeout = timeout(defaultTimeoutSpan)

  override implicit val patienceConfig = PatienceConfig(
    timeout = defaultTimeoutSpan,
    interval = Span(defaultScalaInterval, Millis)
  )

  implicit object FeedbackSampler extends Sample[FeedbackEntry] {
    override def sample: FeedbackEntry = FeedbackEntry(
      gen[UUID],
      gen[String],
      genOpt[String],
      gen[String],
      gen[DateTime]
    )
  }
}
