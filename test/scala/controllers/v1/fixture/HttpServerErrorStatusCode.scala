package scala.controllers.v1.fixture

import org.scalatest.matchers.{ BeMatcher, MatchResult }
import play.api.http.Status

trait HttpServerErrorStatusCode extends Status {

  /*
     * A matcher for HTTP status codes in the range: Server Error 5xx
     */
  class HttpServerErrorStatusCodeMatcher extends BeMatcher[Int] {
    override def apply(left: Int): MatchResult =
      MatchResult(
        left >= INTERNAL_SERVER_ERROR && left <= INSUFFICIENT_STORAGE,
        s"$left was not a HTTP server error status code",
        s"$left was a HTTP server error status code"
      )
  }

  val aServerError = new HttpServerErrorStatusCodeMatcher

}
