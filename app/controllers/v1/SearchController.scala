package controllers.v1

import javax.inject._

import cats.data.ValidatedNel
import com.outworkers.util.catsparsers.{ parse => cparse, _ }
import com.outworkers.util.play._
import com.sksamuel.elastic4s._
import com.typesafe.config.Config
import io.swagger.annotations._
import nl.grons.metrics.scala.DefaultInstrumented
import play.api.libs.json._
import play.api.mvc._
import services.{ BusinessSearchRequest, BusinessService, HBaseCache }

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
class SearchController @Inject() (elastic: ElasticClient, val config: Config, service: BusinessService)(implicit context: ExecutionContext)
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
  def searchTerm(@ApiParam(value = "Query to elastic search") term: String, suggest: Boolean = false): Action[AnyContent] = searchBusiness(Some(term), suggest)

  // public API
  @ApiOperation(
    value = "Search businesses by UBRN",
    notes = "Returns exact business index record for particular UBRN Request",
    httpMethod = "GET"
  )
  def searchBusinessById(@ApiParam(value = "UBRN to search") id: String): Action[AnyContent] = Action.async {
    cparse[Long](id) fold (_.response.future, value =>
      service.findById(value) map {
        case Some(res) =>
          Ok(Json.toJson(res))
        case None =>
          logger.debug(s"Could not find a record with the ID $id")
          NoContent
      })
  }

  // public api
  @ApiOperation(
    value = "Search businesses by query",
    notes = "Returns list of available businesses. Additional parameters: offset, limit, default_operator",
    httpMethod = "GET"
  )
  def searchBusiness(@ApiParam(value = "Query to elastic search") term: Option[String], suggest: Boolean = false): Action[AnyContent] = {
    Action.async { implicit request =>
      requestMeter.mark()

      val searchTerm = term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query"))

      val offset = Try(request.getQueryString("offset").getOrElse("0").toInt).getOrElse(0)
      val limit = Try(request.getQueryString("limit").getOrElse("100").toInt).getOrElse(100)
      val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")
      val failOnQueryError = Try(request.getQueryString("fail_on_bad_query").getOrElse("true").toBoolean).getOrElse(true)

      searchTerm match {
        case Some(query) if query.length > 0 => {
          val searchRequest = BusinessSearchRequest(query, request, suggest)
          // if suggest, match on the BusinessName only, else assume it's an ElasticSearch query
          service.find(searchRequest).map(resp => {
            if (!resp.isEmpty) totalHitsHistogram += resp.totalHits
            response(resp)
          }).recover(responseRecover(query, failOnQueryError))
        }

        case _ => BadRequest.future
      }
    }
  }
}
