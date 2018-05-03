package models

import play.api.mvc.{ AnyContent, Request }

import scala.util.Try

/**
 * Created by coolit on 03/05/2018.
 */
case class BusinessSearchRequest(term: String, offset: Int, limit: Int, suggest: Boolean, defaultOperator: String) {
  override def toString = s"term: $term, offset: $offset, limit: $limit, suggest: $suggest, operator: $defaultOperator"
}

object BusinessSearchRequest {

  def apply(term: String, request: Request[AnyContent], suggest: Boolean = false) = {

    val offset = Try(request.getQueryString("offset").get.toInt).getOrElse(0)
    val limit = Try(request.getQueryString("limit").get.toInt).getOrElse(10000)
    val defaultOperator = request.getQueryString("default_operator").getOrElse("AND")

    new BusinessSearchRequest(term, offset, limit, suggest, defaultOperator)
  }

}
