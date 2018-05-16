package scala.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{ BeforeAndAfterEach, Suite }

trait WithWireMock extends BeforeAndAfterEach { this: Suite =>
  val wireMockPort: Int
  private lazy val wireMockConfig = WireMockConfiguration.wireMockConfig().port(wireMockPort)
  private lazy val wireMockServer = new WireMockServer(wireMockConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.start()
    WireMock.configureFor("localhost", wireMockPort)
  }

  override def afterEach(): Unit = {
    try wireMockServer.stop()
    finally super.afterEach()
  }
}
