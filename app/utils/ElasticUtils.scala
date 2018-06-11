package utils

import javax.inject.Inject

import com.sksamuel.elastic4s.analyzers.{ CustomAnalyzerDefinition, KeywordAnalyzer, LowercaseTokenFilter, WhitespaceTokenizer }
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
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

class ElasticUtils @Inject() (elastic: HttpClient, config: ElasticSearchConfig) extends LazyLogging {

  private val Delimiter = ",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)" // coma, ignore quoted comas
  private val csvFilePath = "conf/demo/sample.csv"
  private val batchSize = 6250
  private val indexType = "business"

  def init(): Unit = {
    removeIndex()
    createNewIndex()
    insertTestData()
  }

  def removeIndex(): Unit = elastic.execute {
    deleteIndex(config.index)
  }.await

  def createNewIndex(): Either[RequestFailure, RequestSuccess[CreateIndexResponse]] = elastic.execute {
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

  // https://stackoverflow.com/questions/4255021/how-do-i-read-a-large-csv-file-with-scala-stream-class
  def insertTestData(): Unit = {
    logger.info(s"Inserting test data [$csvFilePath] into ElasticSearch")
    val t0 = System.currentTimeMillis()
    val iter = Source.fromFile(csvFilePath).getLines()
    val header = splitCsvLine(iter.next)
    logger.info(s"Using first line of file as header: $header")
    val batchInsert = iter.filter(_.trim.nonEmpty).grouped(batchSize).map(batchRows => {
      logger.debug(s"Transforming batch rows of size ${batchRows.length} into Business model")
      val seqMaybeBusiness = batchStrToBusinesses(header, batchRows)
      val flattenedBusinesses = seqMaybeBusiness.flatten
      logger.info(s"Successfully converted ${flattenedBusinesses.length} rows into Business model from total of ${seqMaybeBusiness.length} rows")
      batchInsertIntoElasticSearch(flattenedBusinesses)
    })
    Await.result(Future.sequence(batchInsert), 1 minutes)
    val t1 = System.currentTimeMillis()
    logger.info(s"Inserted records into ElasticSearch in ${t1 - t0} ms")
  }

  def batchStrToBusinesses(header: List[String], batchRows: Seq[String]): Seq[Option[Business]] =
    batchRows.map { rowStr =>
      val rowList = (header zip splitCsvLine(rowStr)).toMap
      Try(Business.fromMap1(rowList("ID").toLong, rowList)).toOption
    }

  def batchInsertIntoElasticSearch(businesses: Seq[Business]): Future[Either[RequestFailure, RequestSuccess[BulkResponse]]] = {
    logger.debug(s"Batch inserting ${businesses.length} businesses into ElasticSearch")
    elastic.execute {
      bulk(
        businesses map { business =>
          indexInto(s"${config.index}/$indexType").id(business.id.toString).fields(Business.toMap(business))
        }
      )
    }
  }

  def unquote(s: String): String = s.replaceAll("\"", "")

  def splitCsvLine(s: String): List[String] = s.split(Delimiter, -1).toList.map(v => unquote(v.trim))
}
