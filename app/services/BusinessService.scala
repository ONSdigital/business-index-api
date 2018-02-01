package services

import javax.inject.Inject

import com.sksamuel.elastic4s.{ RichSearchResponse, _ }
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import dao.{ ElasticSearchDao, HBaseCacheDao }
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.mvc.{ AnyContent, Request }
import uk.gov.ons.bi.models.BusinessIndexRec

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 *
 */

case class SearchResponse(totalHits: Long, maxScore: Float, businesses: Seq[BusinessIndexRec]) {
  val isEmpty = businesses.isEmpty
}

object SearchResponse {
  def apply(richResponse: RichSearchResponse, businesses: Seq[BusinessIndexRec]) = new SearchResponse(richResponse.totalHits, richResponse.maxScore, businesses: Seq[BusinessIndexRec])
  def apply(totalHits: Long, businesses: Seq[BusinessIndexRec]) = new SearchResponse(totalHits, 100f, businesses: Seq[BusinessIndexRec])
  def apply(businesses: Seq[BusinessIndexRec]) = new SearchResponse(businesses.size.toLong, 100f, businesses: Seq[BusinessIndexRec])
}

case class BusinessSearchRequest(term: String, offset: Int, limit: Int, suggest: Boolean, defaultOperator: String) {
  override def toString = s"term: $term, offset: $offset, limit: $limit, suggest: $suggest, operator: $defaultOperator"
}
object BusinessSearchRequest {

  def apply(term: String, request: Request[AnyContent], suggest: Boolean = false) = {

    val offset = Try(request.getQueryString("offset").get.toInt).getOrElse(0)
    val limit = Try(request.getQueryString("limit").get.toInt).getOrElse(100)
    val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")
    val failOnQueryError = Try(request.getQueryString("fail_on_bad_query").get.toBoolean).getOrElse(true)

    new BusinessSearchRequest(term, offset, limit, suggest, defaultOperator)
  }

}

class BusinessService @Inject() (cacheDao: HBaseCacheDao, elasticSearchDao: ElasticSearchDao, conf: Config)(implicit context: ExecutionContext) extends StrictLogging with DefaultInstrumented {

  private[this] val totalHitsHistogram = metrics.histogram("totalHits", "es-searches")

  def find(searchRequest: BusinessSearchRequest): Future[SearchResponse] = if (conf.getBoolean("hbase.caching.enabled")) cacheDao.getFromCache(searchRequest.term).flatMap(_ match {
    case Some(s) => Future(SearchResponse(parse(s)))
    case None => elasticSearchDao.listBusinessesByQuery(searchRequest).flatMap(res => cacheDao.updateCache(searchRequest.term, Json.toJson(res.businesses).toString()).map(_ => res))

  })
  else elasticSearchDao.listBusinessesByQuery(searchRequest)

  def findById(businessId: Long): Future[Option[BusinessIndexRec]] = {
    logger.debug(s"Searching for business with ID $businessId")
    elasticSearchDao.findById(businessId)
  }

  private def parse(r: String): Seq[BusinessIndexRec] = Json.parse(r).validate[Seq[BusinessIndexRec]] match {

    case JsSuccess(b, _) => b
    case JsError(err) => sys.error(s"Error parsing business index list JSON $r -> $err")
    case _ => sys.error(s"Unparseable JSON received: $r")

  }

}
