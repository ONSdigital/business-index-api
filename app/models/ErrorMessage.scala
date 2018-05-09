package models

trait ErrorMessage {
  val msg: String
}

case class InternalServerError(msg: String) extends ErrorMessage
case class ServiceUnavailable(msg: String) extends ErrorMessage
case class GatewayTimeout(msg: String) extends ErrorMessage
