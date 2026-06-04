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

  /** Typed PARAMETER spec emitted at COMPILE time (build-time pure): name/type
    * from the selector (rename ⇒ compile error), default a compile-time literal.
    * Canonical for production builds where the spec must be a pure build artefact.
    * See the Scala-2 `param` scaladoc for the trade-off vs [[paramSpec]]. */
  inline def param[T](id: String, desc: String, default: Any)(sel: T => Any): HardwareSpecification =
    ${ paramCTImpl[T]('id, 'desc, 'default, 'sel) }

  /** Typed PARAMETER spec bound to a config field: name/type from the selector
    * (rename ⇒ compile error), default read from `cfg` at run time (one source).
    * Emits at RUN time; prefer [[param]] when the build must stay run-free. */
  inline def paramSpec[T](id: String, desc: String, cfg: T)(sel: T => Any): HardwareSpecification =
    ${ paramImpl[T]('id, 'desc, 'cfg, 'sel) }

  def paramCTImpl[T: Type](id: Expr[String], desc: Expr[String], default: Expr[Any], sel: Expr[T => Any])(using Quotes): Expr[HardwareSpecification] =
    import quotes.reflect.*
    val idV   = id.value.getOrElse(report.errorAndAbort("param: id must be a string literal"))
    val descV = desc.value.getOrElse(report.errorAndAbort("param: desc must be a string literal"))
    require(!idV.contains(" "), s"Spec ID '$idV' must not contain spaces")
    def digSelect(t: Term): Option[(String, TypeRepr)] = t match
      case Select(_, name)  => Some((name, t.tpe))
      case Typed(e, _)      => digSelect(e)
      case Block(_, e)      => digSelect(e)
      case Inlined(_, _, e) => digSelect(e)
      case _                => None
    val (fname, ftype) = sel.asTerm.underlyingArgument match
      case Lambda(_, body) => digSelect(body).map((n, t) => (n, typeLabel(t.show)))
        .getOrElse(report.errorAndAbort("param selector must be _.field"))
      case _ => report.errorAndAbort("param selector must be a function literal _.field")
    def digLit(t: Term): Option[Any] = t match
      case Literal(c)       => Some(c.value)
      case Typed(e, _)      => digLit(e)
      case Block(_, e)      => digLit(e)
      case Inlined(_, _, e) => digLit(e)
      case _                => None
    val defaultV = digLit(default.asTerm.underlyingArgument).map(String.valueOf).getOrElse(
      report.errorAndAbort(
        "param: default must be a compile-time literal. Use paramSpec[T](id, desc, cfg)(sel) for a runtime config default."))
    val fqn = Fqn.enclosingDeclPath(Symbol.spliceOwner)
    val entries = List("name" -> fname, "type" -> ftype, "default" -> defaultV)
    // build-time emission: runs now, during compilation
    MetaFile.writeSpec(HardwareSpecification(
      id = idV, category = SpecCategory.PARAMETER, description = descV,
      lists = entries, scalaDeclarationPath = fqn))
    val entriesE = Expr(entries)
    '{
      val s = HardwareSpecification(
        id = ${ Expr(idV) }, category = SpecCategory.PARAMETER, description = ${ Expr(descV) },
        lists = ${ entriesE }, scalaDeclarationPath = ${ Expr(fqn) })
      SpecRegistry.addSpec(s)
      s
    }

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
