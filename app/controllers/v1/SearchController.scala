package controllers.v1

import javax.inject._

import cats.data.ValidatedNel
import com.outworkers.util.catsparsers.{ parse => cparse, _ }
import com.outworkers.util.play._
import com.sksamuel.elastic4s.http._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.typesafe.config.Config
import io.swagger.annotations._
import nl.grons.metrics.scala.DefaultInstrumented
import java.util

//import com.sksamuel.elastic4s.{ QueryStringQueryDefinition, SearchDefinition }
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.queries.QueryStringQueryDefinition
import play.api.libs.json
import play.api.libs.json._

import scala.collection.JavaConverters._
import play.api.mvc._
import play.api.libs.json._
import services.{ BusinessSearchRequest, HBaseCache }
import uk.gov.ons.bi.models.{ BIndexConsts, BusinessIndexRec }

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Contains action for the /v1/search route.
 *
 * @param elastic
 * @param context
 * @param config
 */
@Api("Search")
@Singleton
class SearchController @Inject() (elastic: HttpClient, val config: Config)(implicit context: ExecutionContext)
    extends SearchControllerUtils with ElasticDsl with DefaultInstrumented with HBaseCache with ElasticUtils {

  implicit object LongParser extends CatsParser[Long] {
    override def parse(str: String): ValidatedNel[String, Long] = {
      Try(java.lang.Long.parseLong(str)).asValidation
    }
  }

  override protected def tableName = "es_requests"

  // metrics
  private[this] val requestMeter = metrics.meter("search-requests", "requests")
  private[this] val totalHitsHistogram = metrics.histogram("totalHits", "es-searches")

  // public API
  @ApiOperation(
    value = "Search businesses by query",
    notes = "Returns list of available businesses",
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 500, message = "Internal server error"),
    new ApiResponse(code = 503, message = "Elastic search is not available")
  ))
  def searchTerm(@ApiParam(value = "Query to elastic search") term: String, suggest: Boolean = false): Action[AnyContent] = searchBusiness(Some(term))

  // public API
  @ApiOperation(
    value = "Search businesses by UBRN",
    notes = "Returns exact business index record for particular UBRN Request",
    httpMethod = "GET"
  )
  def searchBusinessById(@ApiParam(value = "UBRN to search") id: String): Action[AnyContent] = Action.async {
    elastic.execute {
      search("bi-dev").matchQuery("_id", id)
    } map {
      case Right(r: RequestSuccess[SearchResponse]) => Ok(Json.toJson(BusinessIndexRec.fromRequestSuccessId(r)))
      case Left(f: RequestFailure) => InternalServerError
    }
  }

  // public api
  @ApiOperation(
    value = "Search businesses by query",
    notes = "Returns list of available businesses. Additional parameters: offset, limit, default_operator",
    httpMethod = "GET"
  )
  def searchBusiness(@ApiParam(value = "Query to elastic search") term: Option[String]): Action[AnyContent] = {
    Action.async { implicit request =>
      val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

      val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
      val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)
      val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")
      val failOnQueryError = Try(request.getQueryString("fail_on_bad_query").getOrElse("true").toBoolean).getOrElse(true)

      searchTerm match {
        case Some(query) if query.length > 0 => {
          val searchRequest = BusinessSearchRequest(query, request, false)

          val definition = if (searchRequest.suggest) {
            matchQuery(BIndexConsts.cBiName, query)
          } else {
            QueryStringQueryDefinition(searchRequest.term).defaultOperator(searchRequest.defaultOperator)
          }
          val s: SearchDefinition = search(indexName)
          val withQuery = s.query(definition)
          val started = withQuery.start(searchRequest.offset)
          val limited: SearchDefinition = started.limit(searchRequest.limit)

          elastic.execute(limited).map { resp =>
            resp match {
              case Right(r: RequestSuccess[SearchResponse]) => Ok(Json.toJson(BusinessIndexRec.fromRequestSuccessSearch(r)))
              case Left(f: RequestFailure) => InternalServerError
            }
          }
        }
        case _ => BadRequest.future
      }
    }
  }
}
