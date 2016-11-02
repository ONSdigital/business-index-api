package controllers

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import play.api.mvc.{Action, Controller}

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext

@Singleton
class VersionController @Inject()(implicit exec: ExecutionContext)
  extends Controller with StrictLogging {

  def version = Action {
    Ok(ListMap(BuildInfo.toMap.toSeq.sortBy(_._1):_*)
      .map(i => '"' + i._1 + "\":\"" + i._2 + '"').mkString("{", ", ", "}")
    ).as(JSON)
  }
}
