package scala

import com.typesafe.config.{ Config, ConfigException, ConfigFactory }
import config.{ ElasticSearchConfig, ElasticSearchConfigLoader }
import org.scalatest.{ FreeSpec, Matchers }

/**
 * Created by coolit on 04/05/2018.
 */
class BiConfigManagerTest extends FreeSpec with Matchers {

  private trait ValidFixture {
    val SampleConfiguration: String =
      """|db {
        |  elasticsearch {
        |    index = "bi-dev"
        |    host = "localhost"
        |    port = 9000
        |    ssl = true
        |  }
        |}""".stripMargin
    val config: Config = ConfigFactory.parseString(SampleConfiguration)
  }

  private trait InvalidFixture {
    val SampleConfiguration: String =
      """|db {
        |  elasticsearch {
        |    indexName = "bi-dev"
        |    hostName = "localhost"
        |    portNumber = 9000
        |    ssl = true
        |  }
        |}""".stripMargin
    val config: Config = ConfigFactory.parseString(SampleConfiguration)
  }

  "The config for the HBase REST LocalUnit repository" - {
    "can be successfully loaded when valid" in new ValidFixture {
      ElasticSearchConfigLoader.load(config) shouldBe ElasticSearchConfig("bi-dev", "localhost", 9000, true)
    }

    "will fail fast when incorrect config is provided" in new InvalidFixture {
      a[ConfigException] should be thrownBy {
        ElasticSearchConfigLoader.load(config)
      }
    }
  }
}
