package models

trait ErrorMessage {
  val msg: String
  val status: Int
}

case class InternalServerError(msg: String, status: Int = 500) extends ErrorMessage
case class ServiceUnavailable(msg: String, status: Int = 503) extends ErrorMessage
case class GatewayTimeout(msg: String, status: Int = 504) extends ErrorMessage
case class BadRequest(msg: String, status: Int = 400) extends ErrorMessage
