package controllers.v1

import models.{ Business, ErrorMessage }
import play.api.libs.json.Json.toJson
import play.api.libs.json.Writes
import play.api.mvc.{ Result, Results }

object ControllerResultProcessor extends Results {

  def resultOnFailure(errorMessage: ErrorMessage): Result =
    errorMessage match {
      case _ if errorMessage.msg.startsWith("Timeout") => GatewayTimeout
      case _ => InternalServerError
    }

  def resultOnSuccess[T](optBusiness: Option[T])(implicit writes: Writes[T]): Result =
    optBusiness.fold[Result](NotFound)(business => Ok(toJson(business)))

  def resultSeqOnSuccess(businesses: Seq[Business]): Result = businesses match {
    case Nil => NotFound
    case xs => Ok(toJson(xs))
  }
}

