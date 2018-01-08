package uk.gov.ons.bi

import java.util.concurrent.atomic.AtomicInteger

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import Utils._

/**
 * Created by Volodymyr.Glushak on 08/02/2017.
 */
object CsvProcessor {

  private[this] val logger = LoggerFactory.getLogger(getClass)

  val Delimiter = ",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)" // coma, ignore quoted comas
  val Eol: String = System.lineSeparator

  def splitCsvLine(s: String): List[String] = s.split(Delimiter, -1).toList.map(v => unquote(v.trim))

  def csvToMapToObj[T](csvString: Iterator[String], f: Map[String, String] => T, name: String = "records"): Iterator[T] = {
    val counter = new AtomicInteger(0)

    val header = splitCsvLine(csvString.next)

    val res = csvString.filter(_.trim.nonEmpty).map(dataLine => Future {
      val c = counter.incrementAndGet()
      if (c % 10000 == 0) logger.debug(s"Processed $c $name")
      val data = splitCsvLine(dataLine)
      require(header.length == data.length, s"Data size does not reflect header [${header.length} <> ${data.length}]. \n$header \n$data")
      f(header zip data toMap)
    })
    Await.result(Future.sequence(res), 60 minutes)
  }

  def csvToMap[T](csvString: Iterator[String]) = csvToMapToObj(csvString, identity)
}

