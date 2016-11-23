package models.domain.parsers

import scala.util.Try

trait RecordParser[T <: Product with Serializable] {
  def parse(fields: Map[String, String]): Try[T]
}

object RecordParser {

  implicit def macroMaterialise[T]: RecordParser[T] = macro RecordParserMacro.materialize[T]

  def apply[T : RecordParser]: RecordParser[T] = implicitly[RecordParser[T]]
}