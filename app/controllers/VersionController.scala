package controllers

import javax.inject.Singleton

import play.api.mvc.{Action, Controller}

import scala.collection.immutable.ListMap

/**
  * Contains action for the /version route, displaying the latest BuildInfo values (generated during build).
  */
@Singleton
class VersionController extends Controller {
  def version = Action {
    Ok(ListMap(BuildInfo.toMap.toSeq.sortBy(_._1):_*)
      .map(i => '"' + i._1 + "\":\"" + i._2 + '"').mkString("{", ", ", "}")
    ).as(JSON)
  }
}
