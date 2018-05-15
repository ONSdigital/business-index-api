package controllers.v1

import javax.inject._

import com.outworkers.util.play._
import com.sksamuel.elastic4s.http._
import io.swagger.annotations._
import play.api.mvc._
import models._
import services.BusinessRepository
import controllers.v1.api.BusinessApi
import play.api.libs.json.Json._
import play.api.libs.json.Writes

import play.api.libs.concurrent.Execution.Implicits.defaultContext

@Api("Search")
@Singleton
class BusinessController @Inject() (service: BusinessRepository) extends Controller with ElasticDsl with BusinessApi {

  /**
   * /v1/search/:term - This endpoint can be used like this: /v1/search/BusinessName:test
   */
  override def searchTerm(term: String): Action[AnyContent] = searchBusiness(Some(term))

  /**
   * /v1/business/:id - This will return a single business, with UPRN and VAT/PAYE refs
   */
  override def searchBusinessById(id: Long): Action[AnyContent] = Action.async {
    service.findBusinessById(id).map { errorOrBusiness =>
      errorOrBusiness.fold(resultOnFailure, resultOnSuccess)
    }
  }

  /**
   * /v1/search - This endpoint handles the search query string and returns a list of businesses, with
   * UPRN as null and no VAT/PAYE refs.
   */
  override def searchBusiness(term: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    term.orElse(request.getQueryString("q")).orElse(request.getQueryString("query")) match {
      case Some(query) if query.trim.length > 0 => {
        service.findBusiness(query, request).map { errorOrBusinessSeq =>
          errorOrBusinessSeq.fold(resultOnFailure, resultSeqOnSuccess)
        }
      }
      case _ => BadRequest.future
    }
  }

  /**
   * This endpoint will be called when a request to /v1/business/:id fails the REGEX in the routes file
   */
  def badRequest(a: String) = Action {
    BadRequest
  }

  private def resultOnFailure(errorMessage: ErrorMessage): Result = errorMessage match {
    case g: GatewayTimeout => GatewayTimeout
    case s: ServiceUnavailable => ServiceUnavailable
    case _ => InternalServerError
  }

  private def resultOnSuccess(optBusiness: Option[Business]): Result =
    optBusiness.fold[Result](NotFound)(business => Ok(toJson(business)))

  private def resultSeqOnSuccess(businesses: Seq[Business]): Result =
    if (businesses.isEmpty) NotFound
    else Ok(toJson(businesses))
}
