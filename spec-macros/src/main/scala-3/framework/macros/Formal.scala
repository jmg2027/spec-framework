// spec-macros/src/main/scala-3/framework/macros/Formal.scala
// Scala 3 (inline + quotes) implementation of the formal-connection macros.
// Mirrors the Scala 2 version: bind a PROPERTY/COVERAGE spec to a boolean check,
// emit a Tag recording id ⇐ conditionText ⇐ sourceLocation, and return the
// condition. The spec id is recorded as `@fqn:<declarationPath>` and resolved to
// the real id by SpecCheck at aggregation (specs are emitted at run time on
// Scala 3, so the .spec index is not yet available at macro-expansion time).
package framework.macros

import scala.quoted.*
import framework.spec.{HardwareSpecification, Tag, MetaFile}

object Formal:

  inline def assertProperty(inline spec: HardwareSpecification)(inline cond: Boolean): Boolean =
    ${ implAssert('spec, 'cond) }

  inline def coverProperty(inline spec: HardwareSpecification)(inline cond: Boolean): Boolean =
    ${ implCover('spec, 'cond) }

  def implAssert(spec: Expr[HardwareSpecification], cond: Expr[Boolean])(using Quotes): Expr[Boolean] =
    emit("assert", spec, cond)

  def implCover(spec: Expr[HardwareSpecification], cond: Expr[Boolean])(using Quotes): Expr[Boolean] =
    emit("cover", spec, cond)

  private def emit(kind: String, spec: Expr[HardwareSpecification], cond: Expr[Boolean])(using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    val fqn = Fqn.normalize(spec.asTerm.underlyingArgument.symbol.fullName)
    val pos = Position.ofMacroExpansion
    val exprText = renderInfix(cond.asTerm.underlyingArgument)

    MetaFile.writeTag(
      Tag(
        id                       = "@fqn:" + fqn,
        scalaDeclarationPath     = fqn,
        fullyQualifiedModuleName = fqn,
        hardwareInstancePath     = "",
        srcFile                  = pos.sourceFile.path,
        line                     = pos.startLine + 1,
        column                   = pos.startColumn,
        kind                     = kind,
        expr                     = exprText,
      )
    )
    cond

  private val infixOps =
    Set("+", "-", "*", "/", "%", "<", ">", "<=", ">=", "==", "!=", "&&", "||",
        "&", "|", "^", "<<", ">>", ">>>")

  /** Render a (typed) boolean Term as a clean infix expression. */
  private def renderInfix(using Quotes)(term: quotes.reflect.Term): String =
    import quotes.reflect.*
    def go(t: Term): String = t match
      case Inlined(_, _, e)                                 => go(e)
      case Block(Nil, e)                                    => go(e)
      case Typed(e, _)                                      => go(e)
      case Apply(Select(lhs, op), List(rhs)) if infixOps(op) =>
        s"(${go(lhs)} ${op} ${go(rhs)})"
      case Select(qual, "unary_!")                         => s"!${go(qual)}"
      case Apply(Select(qual, name), args)                 => s"${go(qual)}.${name}(${args.map(go).mkString(", ")})"
      case Select(This(_), name)                           => name
      case Select(qual, name)                              => s"${go(qual)}.${name}"
      case Ident(name)                                     => name
      case Literal(c)                                      => c.value.toString
      case other                                           => other.show(using Printer.TreeShortCode)
    val s = go(term)
    if s.startsWith("(") && s.endsWith(")") then s.drop(1).dropRight(1) else s
