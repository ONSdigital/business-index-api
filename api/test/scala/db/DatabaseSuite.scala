package scala.db

import java.util.concurrent.TimeUnit

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, OptionValues}

import scala.concurrent.ExecutionContextExecutor

trait DatabaseSuite extends FlatSpec with BeforeAndAfterAll with Matchers with OptionValues with ScalaFutures with TestDbProvider {

  protected[this] val defaultScalaTimeoutSeconds = 25

  private[this] val defaultScalaInterval = 50L

  implicit val defaultScalaTimeout = scala.concurrent.duration.Duration(defaultScalaTimeoutSeconds, TimeUnit.SECONDS)

  private[this] val defaultTimeoutSpan = Span(defaultScalaTimeoutSeconds, Seconds)

  implicit val defaultTimeout: PatienceConfiguration.Timeout = timeout(defaultTimeoutSpan)

  implicit val context: ExecutionContextExecutor = com.outworkers.phantom.dsl.context

  override implicit val patienceConfig = PatienceConfig(
    timeout = defaultTimeoutSpan,
    interval = Span(defaultScalaInterval, Millis)
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    val _ = database.create(defaultScalaTimeout)
  }
}
