package models.domain.parsers

trait OffsetProvider[T] {
  def parser: OffsetParser
}
