package scala.utils

import com.typesafe.config.{ Config, ConfigFactory }
import config.ElasticSearchConfigLoader
import org.scalatest.{ FreeSpec, Matchers }
import utils.{ BulkInsertException, ElasticClient, ElasticUtils }

import scala.sample.{ SampleBusiness, SampleConfig }

class ElasticUtilsSpec extends FreeSpec with Matchers with SampleConfig with SampleBusiness {

  private trait ConfigFixture {
    val SampleConfiguration: String =
      s"""|db {
          |  elasticsearch {
          |    username = $SampleUsername
          |    password = $SamplePassword
          |    index = $SampleIndex
          |    host = $SampleHost
          |    port = $SamplePort
          |    ssl = $SampleSsl
          |    loadTestData = $SampleLoad
          |    recreateIndex = $SampleRecreate
          |    csvFilePath = $SampleCsvFilePath
          |  }
          |}""".stripMargin
    val config: Config = ConfigFactory.parseString(SampleConfiguration)
    val esConfig = ElasticSearchConfigLoader.load(config)
  }

  "BatchStrToBusiness" - {
    "can successfully convert a Seq[String] to a Seq[Business]" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      val header = List(
        "ID", "BusinessName", "UPRN", "IndustryCode", "LegalStatus", "TradingStatus", "Turnover", "EmploymentBands",
        "PostCode", "VatRefs", "PayeRefs", "CompanyNo"
      )
      esUtils.batchStrToBusinesses(header, Seq(SampleBusinessString, SampleBusinessString1)).flatten shouldBe
        Seq(SampleBusinessWithAllFields, SampleBusinessWithAllFields1)
    }

    "can successfully convert a Seq[String] to a Seq[Business] and skip erroneous rows" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      val header = List(
        "ID", "BusinessName", "UPRN", "IndustryCode", "LegalStatus", "TradingStatus", "Turnover", "EmploymentBands",
        "PostCode", "VatRefs", "PayeRefs", "CompanyNo"
      )
      esUtils.batchStrToBusinesses(header, Seq(SampleBusinessString, "Tesco")).flatten shouldBe
        Seq(SampleBusinessWithAllFields)
    }
  }

  "Unqoute" - {
    "can unqoute a string with double qoutes" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      esUtils.unquote("""tesco limited""") shouldBe "tesco limited"
    }
  }

  "SplitCsvLine" - {
    "can split a string based upon a delimiter" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      esUtils.splitCsvLine("a,b,c,d") shouldBe List("a", "b", "c", "d")
    }

    "can split a string which includes an array of numbers based upon a delimiter" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      esUtils.splitCsvLine("\"[1,2,3]\",b,c,d") shouldBe List("[1,2,3]", "b", "c", "d")
    }

    "can split a string which includes an array of strings based upon a delimiter" in new ConfigFixture {
      val elasticSearchClient = ElasticClient.getElasticClient(esConfig)
      val esUtils = new ElasticUtils(elasticSearchClient, esConfig)
      esUtils.splitCsvLine(""""["1","2","3"]",b,c,d""") shouldBe List("[1,2,3]", "b", "c", "d")
    }
  }
}
