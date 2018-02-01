package filters

import javax.inject.Inject

import akka.stream.Materializer
import controllers.BuildInfo
import play.api.http.DefaultHttpFilters
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Filter, RequestHeader, Result }
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

class XResponseTimeHeader @Inject() (implicit val mat: Materializer) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val responseTime = endTime - startTime
      val env = sys.props.get("environment").getOrElse("default")

      if (env == "local") {
        result.withHeaders(
          "X-Response-Time" -> responseTime.toString,
          "Server" -> (BuildInfo.name + "/" + BuildInfo.version),
          "Access-Control-Allow-Origin" -> "*",
          "Access-Control-Allow-Methods" -> "OPTIONS, GET, POST, PUT, DELETE, HEAD",
          "Access-Control-Allow-Headers" -> "Accept, Content-Type, Origin, X-Json, X-Prototype-Version, X-Requested-With",
          "Access-Control-Allow-Credentials" -> "true"
        )
      } else {
        result.withHeaders(
          "X-Response-Time" -> responseTime.toString,
          "Server" -> (BuildInfo.name + "/" + BuildInfo.version)
        )
      }
    }
  }
}

class Filters @Inject() (gzipFilter: GzipFilter, responseTimeHeader: XResponseTimeHeader)
  extends DefaultHttpFilters(gzipFilter, responseTimeHeader)