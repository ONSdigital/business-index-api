package controllers.v1

import javax.inject._

import com.outworkers.util.play._
import com.sksamuel.elastic4s.http._
import com.typesafe.config.Config
import com.sksamuel.elastic4s.searches.queries.QueryStringQueryDefinition
import io.swagger.annotations._
import play.api.mvc._
import models._
import services.BusinessService
import ControllerResultProcessor._
import config.ElasticUtils
import controllers.v1.api.BusinessApi

import scala.concurrent.ExecutionContext

@Api("Search")
@Singleton
class BusinessController @Inject() (service: BusinessService, val config: Config)(implicit context: ExecutionContext)
    extends Controller with ElasticDsl with ElasticUtils with BusinessApi {

  /**
   * /v1/search/:term - This endpoint can be used like this: /v1/search/BusinessName:test
   */
  override def searchTerm(term: String): Action[AnyContent] = searchBusiness(Some(term))

  /**
   * /v1/business/:id - This will return a single business, with UPRN and VAT/PAYE refs
   */
  override def searchBusinessById(id: Long): Action[AnyContent] = Action.async {
    service.findBusinessById(id).map { errorOrBusiness =>
      errorOrBusiness.fold(resultOnFailure, resultOnSuccessWithAtMostOneUnit[BusinessIndexRec])
    }
  }

  /**
   * /v1/search - This endpoint handles the search query string and returns a list of businesses, with
   * UPRN as null and no VAT/PAYE refs.
   */
  override def searchBusiness(term: Option[String]): Action[AnyContent] = {
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

  /**
   * This endpoint will be called when a request to /v1/business/:id fails the REGEX in the routes file
   */
  def badRequest(a: String) = Action {
    BadRequest
  }
}
