package filters

import javax.inject.Inject

import akka.stream.Materializer
import controllers.BuildInfo
import play.api.http.DefaultHttpFilters
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Filter, RequestHeader, Result}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

class XResponseTimeHeader @Inject()(implicit val mat: Materializer) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val responseTime = endTime - startTime
      result
        .withHeaders("X-Response-Time" -> responseTime.toString)
        .withHeaders("Server" -> (BuildInfo.name + "/" + BuildInfo.version))
    }
  }
}

class Filters @Inject()(gzipFilter: GzipFilter, responseTimeHeader: XResponseTimeHeader)
  extends DefaultHttpFilters(gzipFilter, responseTimeHeader)