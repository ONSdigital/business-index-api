package controllers

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

@Singleton
class VersionController @Inject()(implicit exec: ExecutionContext)
  extends Controller with StrictLogging {

  def version = Action {
    Ok(BuildInfo.toJson).as("application/json")
  }
}
