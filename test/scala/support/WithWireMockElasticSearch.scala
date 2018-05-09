package scala.support

import com.github.tomakehurst.wiremock.client.{ MappingBuilder, ResponseDefinitionBuilder, WireMock }
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Suite
import play.api.http.Status._

trait WithWireMockElasticSearch extends WithWireMock { this: Suite =>
  override val wireMockPort = 9200

  private val IndexName = "bi-dev"

  def aBusinessQuery(): MappingBuilder =
    createUrlAndThenGetElasticSearchJson()

  private def createUrlAndThenGetElasticSearchJson(): MappingBuilder =
    getElasticSearchJson(s"/$IndexName/_search")

  def getElasticSearchJson(url: String): MappingBuilder =
    post(urlEqualTo(url))

  def anOkResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(OK)

  val stubElasticSearchFor: MappingBuilder => Unit =
    WireMock.stubFor
}
