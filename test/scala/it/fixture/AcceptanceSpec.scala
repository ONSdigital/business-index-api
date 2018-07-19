package scala.it.fixture

import org.scalatest.{ GivenWhenThen, Matchers, fixture }

trait AcceptanceSpec extends fixture.FeatureSpec with GivenWhenThen with Matchers

