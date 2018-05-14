package controllers.v1.api

import models.Business
import play.api.mvc.{ Action, AnyContent }
import io.swagger.annotations.{ ApiOperation, ApiParam, ApiResponse, ApiResponses }

/**
 * To un clutter the BusinessController, Swagger definitions sit inside this trait.
 */
trait BusinessApi {

  @ApiOperation(
    value = "Json representation of the Business results",
    notes = "Requires a query string search term",
    response = classOf[List[Business]],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "No query has been provided (length > 0)"),
    new ApiResponse(code = 404, message = "No businesses for your search term can be found"),
    new ApiResponse(code = 500, message = "The attempt to retrieve search results could not complete due to some failure"),
    new ApiResponse(code = 504, message = "A response was not received from the database within the required time interval")
  ))
  def searchTerm(
    @ApiParam(value = "The QueryString search term", example = "?q=BusinessName:test", required = true) term: String
  ): Action[AnyContent]

  @ApiOperation(
    value = "Json representation of the Business results",
    notes = "Requires a query string search term",
    response = classOf[List[Business]],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "No query has been provided (length > 0)"),
    new ApiResponse(code = 404, message = "No businesses for your search term can be found"),
    new ApiResponse(code = 500, message = "The attempt to retrieve search results could not complete due to some failure"),
    new ApiResponse(code = 504, message = "A response was not received from the database within the required time interval")
  ))
  def searchBusiness(
    @ApiParam(value = "The QueryString search term", example = "?q=BusinessName:test", required = true) term: Option[String]
  ): Action[AnyContent]

  @ApiOperation(
    value = "Json representation of the Business result",
    notes = "Requires an id to search on",
    response = classOf[Business],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "The supplied unique business reference number (UBRN) has an invalid length"),
    new ApiResponse(code = 404, message = "No business id's exist that match the input id"),
    new ApiResponse(code = 500, message = "The attempt to retrieve search results could not complete due to some failure"),
    new ApiResponse(code = 504, message = "A response was not received from the database within the required time interval")
  ))
  def searchBusinessById(
    @ApiParam(value = "Unique Business Reference Number (UBRN)", example = "1234567891123456", required = true) id: Long
  ): Action[AnyContent]
}
