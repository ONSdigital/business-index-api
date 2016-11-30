package models.domain

package object parsers {

  object from {
    def apply(num: Int): NumDsl = NumDsl(num)
  }

  implicit class NumDsl(val num: Int) extends AnyVal {
    def -->(end: Int): OffsetDelimiter = OffsetDelimiter(num, end)
  }


  implicit class StringAugmenter(val str: String) extends AnyVal {
    def offset(num: OffsetDelimiter): (String, OffsetDelimiter) = str -> num
  }
}
