// spec-macros/src/main/scala-3/framework/macros/TypedSpec.scala
// Scala 3 (inline + quotes) implementation of the typed bundle macro.
// Reads each field's name + type from the selector trees (compile-checked
// against T), checks completeness against T's fields, and returns code that
// builds + emits the BUNDLE spec at run time.
package framework.macros

import scala.quoted.*
import framework.spec.{HardwareSpecification, SpecCategory, MetaFile, SpecRegistry}

object TypedSpec:

  inline def bundle[T](inline id: String, inline desc: String, inline uses: String*)(
      inline fields: (T => Any)*): HardwareSpecification =
    ${ bundleImpl[T]('id, 'desc, 'uses, 'fields, '{ true }) }

  inline def bundleLenient[T](inline id: String, inline desc: String, inline uses: String*)(
      inline fields: (T => Any)*): HardwareSpecification =
    ${ bundleImpl[T]('id, 'desc, 'uses, 'fields, '{ false }) }

  /** Typed PARAMETER spec bound to a config field: name/type from the selector
    * (rename ⇒ compile error), default read from `cfg` at run time (one source). */
  inline def paramSpec[T](id: String, desc: String, cfg: T)(sel: T => Any): HardwareSpecification =
    ${ paramImpl[T]('id, 'desc, 'cfg, 'sel) }

  def paramImpl[T: Type](id: Expr[String], desc: Expr[String], cfg: Expr[T], sel: Expr[T => Any])(using Quotes): Expr[HardwareSpecification] =
    import quotes.reflect.*
    def digSelect(t: Term): Option[String] = t match
      case Select(_, name)  => Some(name)
      case Typed(e, _)      => digSelect(e)
      case Block(_, e)      => digSelect(e)
      case Inlined(_, _, e) => digSelect(e)
      case _                => None
    val (fname, ftype) = sel.asTerm.underlyingArgument match
      case Lambda(_, body) => (digSelect(body).getOrElse(report.errorAndAbort("paramSpec selector must be _.field")), typeLabel(body.tpe.show))
      case other           => digSelect(other) match
        case Some(n) => (n, "?")
        case None    => report.errorAndAbort("paramSpec selector must be a function literal _.field")
    val fqn = Fqn.enclosingDeclPath(Symbol.spliceOwner)
    '{
      val _s = framework.spec.Spec.PARAMETER($id).desc($desc)
        .entry("name", ${ Expr(fname) }).entry("type", ${ Expr(ftype) })
        .entry("default", String.valueOf($sel($cfg)))
        .build().copy(scalaDeclarationPath = ${ Expr(fqn) })
      MetaFile.writeSpec(_s)
      _s
    }

  def bundleImpl[T: Type](
      id: Expr[String], desc: Expr[String], uses: Expr[Seq[String]],
      fields: Expr[Seq[T => Any]], strict: Expr[Boolean])(using Quotes): Expr[HardwareSpecification] =
    import quotes.reflect.*

    val idV     = id.valueOrAbort
    val descV   = desc.valueOrAbort
    val isStrict = strict.valueOrAbort
    val usesV   = uses match
      case Varargs(es) => es.map(_.valueOrAbort).toList
      case _           => Nil
    require(!idV.contains(" "), s"Spec ID '$idV' must not contain spaces")

    // -- extract (name, type) from each selector ------------------------------
    def digSelect(t: Term): Option[(String, TypeRepr)] = t match
      case Select(_, name) => Some((name, t.tpe))
      case Typed(e, _)     => digSelect(e)
      case Block(Nil, e)   => digSelect(e)
      case Inlined(_, _, e)=> digSelect(e)
      case _               => None
    def selectOf(t: Term): Option[(String, TypeRepr)] = t match
      case Inlined(_, _, e)                 => selectOf(e)
      case Block(List(d: DefDef), _)        => d.rhs.flatMap(digSelect)
      case Block(Nil, e)                    => selectOf(e)
      case _                                => None

    val fieldExprs = fields match
      case Varargs(es) => es
      case _           => Seq.empty
    val declared: List[(String, String)] = fieldExprs.toList.map { f =>
      selectOf(f.asTerm.underlyingArgument).getOrElse {
        report.errorAndAbort("each field must be a simple member selector like _.addr")
      }
    }.map { case (n, t) => (n, typeLabel(t.show)) }

    // -- completeness against T's public fields -------------------------------
    // own declared fields only, so chisel3's Bundle/Record base fields don't count
    val actual: List[String] =
      TypeRepr.of[T].typeSymbol.declaredFields
        .filterNot(_.flags.is(Flags.Private))
        .map(_.name.trim).filter(_.nonEmpty)
    val undeclared = actual.filterNot(declared.map(_._1).toSet.contains)

    val notes = scala.collection.mutable.ListBuffer("typed-bundle")
    if undeclared.nonEmpty then
      val msg = s"typed bundle '$idV' leaves implementation field(s) undeclared: ${undeclared.mkString(", ")}"
      if isStrict then report.errorAndAbort(msg)
      else
        report.warning(msg)
        notes += ("undeclared-fields: " + undeclared.mkString(", "))

    // declaration path of the enclosing val
    val fqn = Fqn.enclosingDeclPath(Symbol.spliceOwner)

    // -- runtime build + emit -------------------------------------------------
    val listsE = Expr(declared)
    val usesE  = Expr(usesV)
    val notesE = Expr(notes.toList)
    '{
      val s = HardwareSpecification(
        id = ${ Expr(idV) },
        category = SpecCategory.BUNDLE,
        description = ${ Expr(descV) },
        uses = ${ usesE }.toSet,
        lists = ${ listsE },
        notes = ${ notesE },
        scalaDeclarationPath = ${ Expr(fqn) },
      )
      MetaFile.writeSpec(s)
      SpecRegistry.addSpec(s)
      s
    }

  private def typeLabel(s: String): String = s.replaceAll("^.*\\.", "")
