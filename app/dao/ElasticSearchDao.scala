package dao

import javax.inject.Inject

import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import controllers.v1.SearchControllerUtils
import play.api.libs.json._
import services.{BusinessSearchRequest, SearchResponse}
import uk.gov.ons.bi.models.{BIndexConsts, BusinessIndexRec}

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
class ElasticSearchDao @Inject() (elastic: ElasticClient, config: Config)(implicit context: ExecutionContext) extends ElasticDsl with StrictLogging with SearchControllerUtils {

  implicit object BusinessHitAs extends HitAs[BusinessIndexRec] {
    override def as(hit: RichSearchHit): BusinessIndexRec = {
      // val bi: Try[BusinessIndexRec] = {
      val str = hit.sourceAsString
      val js = Json.parse(str).as[JsObject] + ("id", JsNumber(hit.getId.toLong))
      Json.fromJson[BusinessIndexRec](js) match {
        case JsSuccess(buss, _) => buss
        case JsError(e) => {
          logger.error(s"failed to create businessIndex from json: ${js.toString}")
          throw new Exception(s" jason parsing error: ${e.mkString}")
        }

      }

      //}
      //bi.getOrElse(BusinessIndexRec.fromMap(hit.id.toLong, hit.sourceAsMap))
    }
  }

  val indexName: String = config.getString("elasticsearch.bi.name").concat("/business")

  def findById(businessId: Long): Future[Option[BusinessIndexRec]] = {
    logger.debug(s"Searching for business with ID $id")
    elastic.execute {
      get.id(businessId).from(indexName)
    } map (r => resultAsBusiness(businessId, r))
  }

  def listBusinessesByQuery(searchRequest: BusinessSearchRequest): Future[SearchResponse] = {
    import searchRequest._

    val definition = if (suggest) {
      matchQuery(BIndexConsts.cBiName, query)
    } else {
      QueryStringQueryDefinition(searchRequest.term).defaultOperator(defaultOperator)
    }
    val s: SearchDefinition = search.in(indexName)
    val withQuery = s.query(definition)
    val started = withQuery.start(offset)
    val limited: SearchDefinition = started.limit(limit)

    elastic.execute(limited).map { resp =>

      if (resp.shardFailures.nonEmpty)
        sys.error(s"${resp.shardFailures.length} failed shards out of ${resp.totalShards}, the returned result would be partial and not reliable")
      resp
    }.map { resp =>
      {
        logger.trace(s"Business search $searchRequest - response: $resp")
        val businesses: Seq[BusinessIndexRec] = resp.as[BusinessIndexRec]
        val response = SearchResponse(resp, businesses)
        response
      }
    }
  }

}
