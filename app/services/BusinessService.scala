package services

import com.sksamuel.elastic4s.searches.SearchDefinition
import models.{ BusinessIndexRec, ErrorMessage }

import scala.concurrent.Future

trait BusinessService {
  def findBusiness(query: SearchDefinition): Future[Either[ErrorMessage, Option[List[BusinessIndexRec]]]]
  def findBusinessById(id: Long): Future[Either[ErrorMessage, Option[BusinessIndexRec]]]
}