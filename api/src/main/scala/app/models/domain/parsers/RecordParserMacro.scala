package models.domain.parsers


@macrocompat.bundle
class RecordParserMacro(val c: scala.reflect.macros.blackbox.Context) {

  import c.universe._

  def typed[A : c.WeakTypeTag]: Symbol = weakTypeOf[A].typeSymbol

  def fields(tpe: Type): Iterable[(Name, Type)] = {
    object CaseField {
      def unapply(arg: TermSymbol): Option[(Name, Type)] = {
        if (arg.isVal && arg.isCaseAccessor) {
          Some(TermName(arg.name.toString.trim) -> arg.typeSignature)
        } else {
          None
        }
      }
    }

    tpe.decls.collect { case CaseField(name, fType) => name -> fType }
  }

  val pkg = q"models.domain.parsers"

  def extractorTerm(source: TermName): TermName = {
    TermName(source.decodedName.toString + "Opt")
  }

  case class Extractor(
    term: TermName,
    parser: Tree
  )

  def materialize[T : c.WeakTypeTag]: Tree = {
    val sourceTerm = TermName("source")

    val tpe = weakTypeOf[T]
    val source = tpe.typeSymbol.companion.name.toTermName

    val extractors = fields(tpe) map { case (field, fieldTpe) =>

      val term = extractorTerm(field.toTermName)

      Extractor(
        term,
        fq"""$term <- $pkg.RecordParser[$fieldTpe].parse($sourceTerm)"""
      )
    }

    val tree = q"""
      for (..${extractors.map(_.parser)}) yield $source.apply(..${extractors.map(_.term)})
    """

    Console.println(showCode(tree))

    tree
  }

}