package controllers

import javax.inject.Singleton

import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}
import play.api.mvc.{Action, Controller}

import scala.collection.immutable.ListMap

/**
  * Contains action for the /version route, displaying the latest BuildInfo values (generated during build).
  */
@Api("Utils")
@Singleton
class VersionController extends Controller {
  // public api
  @ApiOperation(value = "Version List",
    notes = "Provides a full listing of all versions of software related tools - this can be found in the build file.",
    httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Success - Displays a version list as json.")))
  def version = Action {
    Ok(ListMap(BuildInfo.toMap.toSeq.sortBy { case (k, _) => k }: _*)
      .map { case (k, v) => s""" "$k":"$v" """.trim }.mkString("{", ", ", "}")
    ).as(JSON)
  }
}
