package models

case class BusinessSearchRequest(term: String, offset: Int, limit: Int, defaultOperator: String)
