package scala.utils

import org.scalatest.{ FreeSpec, Matchers }
import utils.EitherSupport

class EitherSupportSpec extends FreeSpec with Matchers {

  "A sequence of Eithers" - {
    "can be transformed into a single" - {
      "Right containing a sequence of values when all are Rights" in {
        EitherSupport.sequence(Seq(Right(1), Right(2), Right(3))) shouldBe Right(Seq(1, 2, 3))
      }

      "Right containing an empty sequence when empty" in {
        EitherSupport.sequence(Seq.empty) shouldBe Right(Seq.empty)
      }

      "Left when any is a Left" in {
        EitherSupport.sequence(Seq(Right(1), Left("Failure Message"), Right(3))) shouldBe Left("Failure Message")
      }
    }
  }
}
