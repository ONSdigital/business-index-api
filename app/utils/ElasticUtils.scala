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

  private val csvFilePath = "conf/demo/sample.csv"

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

  // https://stackoverflow.com/questions/4255021/how-do-i-read-a-large-csv-file-with-scala-stream-class
  def insertTestData(): Unit = { // Either[RequestFailure, RequestSuccess[BulkResponse]] = {
    logger.info(s"Inserting test data [$csvFilePath] into ElasticSearch")

    val t0 = System.currentTimeMillis()
    val iter = Source.fromFile(csvFilePath).getLines()
    val header = splitCsvLine(iter.next)

    logger.info(s"Using first line of file as header: $header")

    val gr = iter.filter(_.trim.nonEmpty).grouped(6250).map(batchRows => {
      logger.debug(s"Transforming batch of size ${batchRows.length} into Business model")
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
    val t1 = System.currentTimeMillis()
    logger.error("Elapsed time: " + (t1 - t0) + "ms")
    // batch 10000     6403ms
    // batch 7500      5751ms
    // batch 6250      5658ms
    // batch 5000      5751ms
    // batch 2000      6118ms
    // batch 1000      6233ms
    // batch 500       6435ms
    // batch 100       error
    throw new Exception("Error")
  }
}
