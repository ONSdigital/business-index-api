package scala.support

import org.scalatest.Outcome
import play.api.http.Port
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

trait WsClientFixture extends org.scalatest.fixture.TestSuite {
  override type FixtureParam = WSClient

  def wsPort: Port

  override protected def withFixture(test: OneArgTest): Outcome = {
    WsTestClient.withClient { wsClient =>
      withFixture(test.toNoArgTest(wsClient))
    }(wsPort)
  }
}
