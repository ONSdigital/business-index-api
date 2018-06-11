package utils

object EitherSupport {
  /**
   * Collapses a sequence of Eithers into a single Either that represents either the first Left encountered,
   * or a sequence of all the Right values.
   *
   * @param eithers zero to many Either[A, B] instances
   * @tparam A the type of the Left
   * @tparam B the type of the Right
   * @return either a Left containing the first Left value in eithers; or a Right containing a sequence of all the
   *         Right values in eithers
   */
  /*
   * Additional Notes:
   * - sequence is a common functional idiom that can be implemented generically with Applicative / Traversable Functors.
   *   It is a special case of the traverse function - see https://typelevel.org/cats/typeclasses/traverse.html for
   *   more details, or see scala.concurrent.Future for another example of sequence.
   * - this implementation will continue to visit all the eithers (because it uses fold) even if the first is a Left.
   *   If profiling flags this as a performance issue, we can switch to using a custom tail-recursive function that
   *   would allow us to abort the traversal on the first Left.
   */
  def sequence[A, B](eithers: Seq[Either[A, B]]): Either[A, Seq[B]] = {
    val zeroAcc: Either[A, Seq[B]] = Right(Seq.empty[B])
    eithers.foldRight(zeroAcc) { (e, acc) =>
      acc.right.flatMap { bs =>
        e.fold(a => Left(a), b => Right(b +: bs))
      }
    }
  }
}
