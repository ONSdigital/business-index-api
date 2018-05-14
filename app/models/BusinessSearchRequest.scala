package models

import play.api.mvc.{ AnyContent, Request }

import scala.util.Try

case class BusinessSearchRequest(term: String, offset: Int, limit: Int, suggest: Boolean, defaultOperator: String)

object BusinessSearchRequest {
  def apply(term: String, request: Request[AnyContent], suggest: Boolean = false) = {
    val offset = request.getQueryString("offset").flatMap(str => Try(str.toInt).toOption).getOrElse(0)
    val limit = request.getQueryString("limit").flatMap(str => Try(str.toInt).toOption).getOrElse(10000)
    val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")
    new BusinessSearchRequest(term, offset, limit, suggest, defaultOperator)
  }
}
