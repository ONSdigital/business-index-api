package models.domain

package object parsers {


  implicit class NumDsl(val num: Int) extends AnyVal {
    def -->(end: Int): OffsetDelimiter = OffsetDelimiter(num, end)
  }


  implicit class StringAugmenter(val str: String) extends AnyVal {
    def offset(num: OffsetDelimiter): (String, OffsetDelimiter) = str -> num
  }
}
