package scala.it.fixture

import org.scalatest.{ GivenWhenThen, Matchers, fixture }

/**
 * Created by coolit on 08/05/2018.
 */
trait AcceptanceSpec extends fixture.FeatureSpec with GivenWhenThen with Matchers

