package scala.support

import com.github.tomakehurst.wiremock.client.{ MappingBuilder, ResponseDefinitionBuilder, WireMock }
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.Suite
import play.api.http.Status._

/**
 * Created by coolit on 08/05/2018.
 */
trait WithWireMockElasticSearch extends WithWireMock { this: Suite =>
  override val wireMockPort = 9200

  private val IndexName = "bi-dev"

  def aBusinessIdRequest(withId: Long): MappingBuilder =
    aBusinessQuery(withId)

  private def aBusinessQuery(id: Long): MappingBuilder =
    createUrlAndThenGetElasticSearchJson(id)

  private def createUrlAndThenGetElasticSearchJson(id: Long): MappingBuilder =
    getElasticSearchJson(s"/$IndexName/_search")

  def getElasticSearchJson(url: String): MappingBuilder =
    post(urlEqualTo(url))

  def anOkResponse(): ResponseDefinitionBuilder =
    aResponse().withStatus(OK)

  val stubElasticSearchFor: MappingBuilder => Unit =
    WireMock.stubFor
}
