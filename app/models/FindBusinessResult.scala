package models

case class FindBusinessResult(businesses: Seq[Business], numUncappedResults: Long)