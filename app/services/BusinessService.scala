package services

import models.{ Business, BusinessSearchRequest, ErrorMessage }

import scala.concurrent.Future

trait BusinessRepository {
  def findBusiness(query: BusinessSearchRequest): Future[Either[ErrorMessage, Seq[Business]]]
  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]]
}