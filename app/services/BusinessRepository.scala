package services

import models.{ Business, BusinessSearchRequest, ErrorMessage, FindBusinessResult }

import scala.concurrent.Future

trait BusinessRepository {
  def findBusiness(query: BusinessSearchRequest): Future[Either[ErrorMessage, FindBusinessResult]]
  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]]
}