package services

import models.{ BusinessIndexRec, ErrorMessage }
import play.api.mvc.{ Request, AnyContent }

import scala.concurrent.Future

trait BusinessService {
  def findBusiness(query: String, request: Request[AnyContent]): Future[Either[ErrorMessage, Option[List[BusinessIndexRec]]]]
  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[BusinessIndexRec]]]
}