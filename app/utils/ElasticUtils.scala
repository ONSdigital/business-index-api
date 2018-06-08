package utils

import javax.inject.Inject

import com.sksamuel.elastic4s.analyzers.{ CustomAnalyzerDefinition, KeywordAnalyzer, LowercaseTokenFilter, WhitespaceTokenizer }
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.index.{ CreateIndexResponse, IndexResponse }
import com.sksamuel.elastic4s.http.{ HttpClient, RequestFailure, RequestSuccess }
import com.typesafe.scalalogging.LazyLogging
import config.ElasticSearchConfig
import models.Business
import models.IndexConsts._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Try

/**
 * Created by coolit on 29/05/2018.
 */
class ElasticUtils @Inject() (elastic: HttpClient, config: ElasticSearchConfig) extends LazyLogging {

  def init(): Unit = {
    removeIndex()
    createNewIndex()
    insertTestData()
  }

  // We use Unit here as we don't care if the request fails
  def removeIndex(): Unit = elastic.execute {
    deleteIndex(config.index)
  }.await

  // Next, we need to create the index with the correct mappings/analyzers etc.
  // Changes to index creation:
  // - cannot use a KeywordAnalyzer on a long field
  // - using KeywordAnalyzer instead of NotAnalyzed - achieves the same thing?
  // - what does includeInAll do?
  def createNewIndex(): Either[RequestFailure, RequestSuccess[CreateIndexResponse]] = {
    elastic.execute {
      createIndex(config.index).analysis(
        CustomAnalyzerDefinition(cBiAnalyzer, WhitespaceTokenizer, LowercaseTokenFilter)
      ).mappings(
          mapping(cBiType).fields(
            textField(cBiName).boost(cBiNameBoost).analyzer(cBiAnalyzer),
            longField(cBiUprn),
            textField(cBiPostCode).analyzer(cBiAnalyzer),
            longField(cBiIndustryCode),
            textField(cBiLegalStatus).analyzer(KeywordAnalyzer),
            textField(cBiTradingStatus).analyzer(KeywordAnalyzer),
            textField(cBiTurnover).analyzer(cBiAnalyzer),
            textField(cBiEmploymentBand).analyzer(cBiAnalyzer),
            textField(cBiPayeRefs).analyzer(KeywordAnalyzer),
            longField(cBiVatRefs),
            textField(cBiCompanyNo).analyzer(KeywordAnalyzer)
          )
        )
    }.await
  }

  val Delimiter = ",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)" // coma, ignore quoted comas

  def unquote(s: String): String = s.replaceAll("\"", "")

  def splitCsvLine(s: String): List[String] = s.split(Delimiter, -1).toList.map(v => unquote(v.trim))

  def insertTestData(): Unit = { // Either[RequestFailure, RequestSuccess[BulkResponse]] = {
    logger.info("Insert test data")
    // https://stackoverflow.com/questions/4255021/how-do-i-read-a-large-csv-file-with-scala-stream-class

    // Load in 1000 records from the CSV file
    val csvFilePath = "conf/demo/sample.csv"
    val src = Source.fromFile(csvFilePath)
    val iter = src.getLines()

    val header = splitCsvLine(iter.next)

    logger.info(s"header: ${header}")

    val gr = iter.filter(_.trim.nonEmpty).grouped(1000).map(batchRows => {
      logger.debug(s"processing dataLine...")
      val res = batchRows.map { rowStr =>
        val rowList = splitCsvLine(rowStr)
        val csvRowMap = (header zip rowList).toMap
        Try(Business.fromMap1(csvRowMap("ID").toLong, csvRowMap)).toOption
      }
      elastic.execute {
        bulk(
          res.flatten map { business =>
            indexInto(s"${config.index}/business").id(business.id.toString).fields(Business.toMap(business))
          }
        )
      }
    })
    val p = Await.result(Future.sequence(gr), 5 minutes)
    throw new Exception("Error")
  }
}
