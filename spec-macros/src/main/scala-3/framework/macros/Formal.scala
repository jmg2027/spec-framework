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

  // `cond` is generic so the same macro works for a Scala `Boolean` and a
  // `chisel3.Bool` (or any condition type).

  // `cond` is by-value (not inline) so its tree keeps the source positions we
  // need to record the condition verbatim.
  inline def assertProperty[T](inline spec: HardwareSpecification)(cond: T): T =
    ${ implAssert('spec, 'cond) }

  inline def coverProperty[T](inline spec: HardwareSpecification)(cond: T): T =
    ${ implCover('spec, 'cond) }

  def implAssert[T: Type](spec: Expr[HardwareSpecification], cond: Expr[T])(using Quotes): Expr[T] =
    emit("assert", spec, cond)

  def implCover[T: Type](spec: Expr[HardwareSpecification], cond: Expr[T])(using Quotes): Expr[T] =
    emit("cover", spec, cond)

  private def emit[T: Type](kind: String, spec: Expr[HardwareSpecification], cond: Expr[T])(using Quotes): Expr[T] =
    import quotes.reflect.*

    val fqn = Fqn.normalize(spec.asTerm.underlyingArgument.symbol.fullName)
    val pos = Position.ofMacroExpansion
    // Record the verbatim source of the condition (Chisel desugars `a && b` into
    // an unreadable `a.do_&&(b)(using SourceInfo)`); fall back to pretty-printing.
    val condTerm = cond.asTerm.underlyingArgument
    val exprText =
      condTerm.pos.sourceCode
        .map(_.trim)
        .getOrElse(renderInfix(condTerm))

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
