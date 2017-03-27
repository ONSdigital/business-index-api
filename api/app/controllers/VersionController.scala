package controllers

import javax.inject.Singleton

import io.swagger.annotations.Api
import play.api.mvc.{Action, Controller}

import scala.collection.immutable.ListMap

/**
  * Contains action for the /version route, displaying the latest BuildInfo values (generated during build).
  */
@Api("Utils")
@Singleton
class VersionController extends Controller {
  def version = Action {
    Ok(ListMap(BuildInfo.toMap.toSeq.sortBy { case (k, _) => k }: _*)
      .map { case (k, v) => s""" "$k":"$v" """.trim }.mkString("{", ", ", "}")
    ).as(JSON)
  }
}
