package scala.it.fixture

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Port
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

import scala.support.WsClientFixture

class ServerAcceptanceSpec extends AcceptanceSpec with WsClientFixture with GuiceOneServerPerSuite with DefaultAwaitTimeout with FutureAwaits {
  override def wsPort: Port =
    new Port(port)
}
