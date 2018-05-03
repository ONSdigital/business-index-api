package controllers.v1

import models.ErrorMessage
import play.api.libs.json.Json.toJson
import play.api.libs.json.Writes
import play.api.mvc.{ Result, Results }

object ControllerResultProcessor extends Results {

  def resultOnFailure(errorMessage: ErrorMessage): Result =
    errorMessage match {
      case _ if errorMessage.msg.startsWith("Timeout") => GatewayTimeout
      case _ => InternalServerError
    }

  def resultOnSuccessWithAtMostOneUnit[T](optUnit: Option[T])(implicit writes: Writes[T]): Result =
    optUnit.fold[Result](NotFound)(unit => Ok(toJson(unit)))
}

