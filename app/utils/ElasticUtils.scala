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

case class BulkInsertException(msg: String) extends Exception
case class CreateIndexException(msg: String) extends Exception

class ElasticUtils @Inject() (elastic: HttpClient, config: ElasticSearchConfig) extends LazyLogging {

  private val Delimiter = ",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)" // coma, ignore quoted comas
  private val csvFilePath = config.csvFilePath
  private val batchSize = 6250 // 6250 gave the best results after testing between 100 and 10,000
  private val indexType = "business"

  def recreateIndex(): Unit = {
    removeIndex() // We don't care whether this works or not
    createNewIndex() match {
      case Left(f: RequestFailure) =>
        throw CreateIndexException(s"Creation of ElasticSearch index failed with status: ${f.status}")
      case Right(s: RequestSuccess[CreateIndexResponse]) =>
        logger.info(s"Successfully created ElasticSearch index [${config.index}]")
    }
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

  def insertTestData(): Unit = {
    logger.info(s"Inserting test data [$csvFilePath] into ElasticSearch")
    val t0 = System.currentTimeMillis()
    val iter = Source.fromFile(csvFilePath).getLines()
    val header = splitCsvLine(iter.next)
    logger.info(s"Using first line of file as header: $header")
    val batchInsert = iter.filter(_.trim.nonEmpty).grouped(batchSize).map(batchRows => {
      val seqMaybeBusiness = batchStrToBusinesses(header, batchRows)
      val flattenedBusinesses = seqMaybeBusiness.flatten
      logger.debug(s"Successfully converted ${flattenedBusinesses.length} rows into Business model from total of ${seqMaybeBusiness.length} rows")
      batchInsertIntoElasticSearch(flattenedBusinesses)
    })
    val res = Await.result(Future.sequence(batchInsert), 1 minutes).toList
    val t1 = System.currentTimeMillis()
    logger.info(s"Inserted records into ElasticSearch in ${t1 - t0} ms")
    EitherSupport.sequence(res) match {
      case Left(f: RequestFailure) => throw BulkInsertException(f.error.reason)
      case Right(s: Seq[RequestSuccess[BulkResponse]]) =>
        logger.info(s"Successfully inserted ${s.length} batches of $batchSize records into ElasticSearch.")
    }
  }

  def batchStrToBusinesses(header: List[String], batchRows: Seq[String]): Seq[Option[Business]] =
    batchRows.map { rowStr =>
      val rowList = (header zip splitCsvLine(rowStr)).toMap
      Try(Business.fromCSVMap(rowList("ID").toLong, rowList)).toOption
    }

  def batchInsertIntoElasticSearch(businesses: Seq[Business]): Future[Either[RequestFailure, RequestSuccess[BulkResponse]]] = {
    logger.info(s"Batch inserting ${businesses.length} businesses into ElasticSearch")
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
