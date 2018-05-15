package scala.support

object JsonString {
  def string(name: String, value: String): Option[String] =
    Some(s""""$name":"$value"""")

  def optionalString(name: String, optValue: Option[String]): Option[String] =
    optValue.flatMap(string(name, _))

  def int(name: String, value: Int): Option[String] =
    Some(s""""$name":$value""")

  def long(name: String, value: Long): Option[String] =
    Some(s""""$name":$value""")

  def seqStr(name: String, value: Seq[String]): Option[String] = {
    val s = value.map(x => s""""$x"""")
    Some(s""""$name":[${s.mkString(",")}]""")
  }

  def optionalInt(name: String, optValue: Option[Int]): Option[String] =
    optValue.flatMap(int(name, _))

  def optionalLong(name: String, optValue: Option[Long]): Option[String] =
    optValue.flatMap(long(name, _))

  def optionalSeqString(name: String, optValue: Option[Seq[String]]): Option[String] =
    optValue.flatMap(seqStr(name, _))

  def withValues(values: Option[String]*): String =
    values.flatten.mkString(",")

  def withObject(values: Option[String]*): String =
    values.flatten.mkString("{", ",", "}")
}
