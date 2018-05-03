package controllers.v1

import javax.inject._

import com.outworkers.util.play._
import com.sksamuel.elastic4s.http._
import com.typesafe.config.Config
import io.swagger.annotations._
import nl.grons.metrics.scala.DefaultInstrumented
import com.sksamuel.elastic4s.searches.queries.QueryStringQueryDefinition
import play.api.mvc._
import models._
import services.BusinessService
import ControllerResultProcessor._

import scala.concurrent.ExecutionContext

@Api("Search")
@Singleton
class SearchController @Inject() (service: BusinessService, val config: Config)(implicit context: ExecutionContext)
    extends Controller with ElasticDsl with DefaultInstrumented with ElasticUtils {

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
  def searchBusinessById(@ApiParam(value = "UBRN to search") id: Long): Action[AnyContent] = Action.async {
    service.findBusinessById(id).map { errorOrBusiness =>
      errorOrBusiness.fold(resultOnFailure, resultOnSuccessWithAtMostOneUnit[BusinessIndexRec])
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

      searchTerm match {
        case Some(query) if query.length > 0 => {
          val searchRequest = BusinessSearchRequest(query, request)
          val definition = QueryStringQueryDefinition(searchRequest.term).defaultOperator(searchRequest.defaultOperator)
          val searchQuery = search(indexName).query(definition).start(searchRequest.offset).limit(searchRequest.limit)

          service.findBusiness(searchQuery).map { errorOrBusinessList =>
            errorOrBusinessList.fold(resultOnFailure, resultOnSuccessWithAtMostOneUnit[List[BusinessIndexRec]])
          }
        }
        case _ => BadRequest.future
      }
    }
  }

  def badRequest(a: String) = Action {
    BadRequest
  }
}
