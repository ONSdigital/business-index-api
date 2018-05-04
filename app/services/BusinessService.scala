package services

import models.{ Business, ErrorMessage }
import play.api.mvc.{ Request, AnyContent }

import scala.concurrent.Future

trait BusinessService {
  def findBusiness(query: String, request: Request[AnyContent]): Future[Either[ErrorMessage, Option[List[Business]]]]
  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[Business]]]
}