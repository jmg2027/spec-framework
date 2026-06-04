// spec-macros/src/main/scala/framework/macros/Formal.scala
// -----------------------------------------------------------------------------
//  Formal / assertion connection  ("formal 연결")
// -----------------------------------------------------------------------------
//  A PROPERTY (or COVERAGE) spec node used to be pure prose:
//
//      PROPERTY("PROP_IN_ORDER_FETCH_RESPONSE").desc("responses are in order")
//
//  …documented, but bound to nothing. There was no machine-checkable link between
//  the stated property and any check in the RTL, so the index could not answer
//  "which declared properties are actually enforced?".
//
//  `assertProperty` / `coverProperty` close that gap. They wrap a real boolean
//  condition, emit a `Tag` (kind = "assert" / "cover") that records the property
//  id, the source location, AND the source text of the bound condition, and then
//  return the condition unchanged so it can drive an actual assert/cover in the
//  hardware:
//
//      assertProperty(propInOrder) { reqCount >= respCount }
//
//  Downstream, the checker compares the set of declared PROPERTY/COVERAGE nodes
//  against the set that carry such a binding and reports the *formal coverage* —
//  unenforced properties become visible instead of rotting silently.
// -----------------------------------------------------------------------------
package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import framework.spec.{HardwareSpecification, Tag, MetaFile, SpecIndex}

object Formal {

  /** Bind PROPERTY `spec` to assertion `cond`; returns `cond` for wiring. */
  def assertProperty(spec: HardwareSpecification)(cond: Boolean): Boolean = macro implAssert

  /** Bind COVERAGE `spec` to cover point `cond`; returns `cond` for wiring. */
  def coverProperty(spec: HardwareSpecification)(cond: Boolean): Boolean = macro implCover

  def implAssert(c: blackbox.Context)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[Boolean],
  ): c.Expr[Boolean] = emit(c)("assert")(spec)(cond)

  def implCover(c: blackbox.Context)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[Boolean],
  ): c.Expr[Boolean] = emit(c)("cover")(spec)(cond)

  private def emit(c: blackbox.Context)(kind: String)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[Boolean],
  ): c.Expr[Boolean] = {
    import c.universe._
    def abort(m: String): Nothing = c.abort(c.enclosingPosition, m)

    // Resolve the property id the same way @LocalSpec does: prefer the .spec
    // index keyed by the spec val's fully-qualified name, then fall back to
    // compile-time evaluation of `spec.id`.
    val checked = c.typecheck(spec.tree.duplicate, c.TERMmode)
    val symFqn  = Option(checked.symbol).map(_.fullName).getOrElse("")
    val id: String =
      SpecIndex.idFor(symFqn).getOrElse {
        try c.eval(c.Expr[String](c.untypecheck(q"$spec.id")))
        catch {
          case e: Throwable =>
            abort(s"assertProperty/coverProperty: cannot resolve spec id for '$symFqn': ${e.getMessage}")
        }
      }

    val pos = c.enclosingPosition
    // Render the bound condition into clean infix form (valid for the report AND
    // directly usable as an SVA expression): `(reqCount - respCount) <= maxOutstanding`.
    // The typed tree uses method-call form (`a.<=(b)`) and synthetic `This`
    // qualifiers, so we walk it and re-emit operators infix.
    val exprText = renderInfix(c)(cond.tree)

    val tag = Tag(
      id                       = id,
      scalaDeclarationPath     = c.internal.enclosingOwner.fullName,
      fullyQualifiedModuleName = c.internal.enclosingOwner.fullName,
      hardwareInstancePath     = "",
      srcFile                  = pos.source.path,
      line                     = pos.line,
      column                   = pos.column,
      kind                     = kind,
      expr                     = exprText,
    )
    MetaFile.writeTag(tag)
    c.info(pos, s"[Formal] bound $kind for property '$id'", force = true)

    // Return the original condition unchanged: runtime/sim/formal semantics are
    // whatever the caller wires it to (e.g. chisel3.assert(assertProperty(p){c})).
    c.Expr[Boolean](q"${cond.tree}")
  }

  // Set of symbolic operators rendered infix (everything else stays method form).
  private val infixOps =
    Set("+", "-", "*", "/", "%", "<", ">", "<=", ">=", "==", "!=", "&&", "||",
        "&", "|", "^", "<<", ">>", ">>>")

  /** Pretty-print a (typed) boolean tree as a clean infix expression. */
  private def renderInfix(c: blackbox.Context)(tree: c.universe.Tree): String = {
    import c.universe._
    def go(t: Tree): String = t match {
      case Block(Nil, e)                       => go(e)
      case Typed(e, _)                         => go(e)
      case Apply(Select(lhs, op), List(rhs)) if infixOps(op.decodedName.toString) =>
        s"(${go(lhs)} ${op.decodedName.toString} ${go(rhs)})"
      case Select(qual, op) if op.decodedName.toString == "unary_!" => s"!${go(qual)}"
      case Apply(Select(qual, name), args)     => s"${go(qual)}.${name.decodedName.toString}(${args.map(go).mkString(", ")})"
      case Select(This(_), name)               => name.decodedName.toString
      case Select(qual, name)                  => s"${go(qual)}.${name.decodedName.toString}"
      case Ident(name)                         => name.decodedName.toString
      case Literal(Constant(v))                => v.toString
      case other                               => showCode(other).replaceAll("""\b[A-Za-z_][A-Za-z0-9_]*\.this\.""", "")
    }
    val s = go(tree)
    if (s.startsWith("(") && s.endsWith(")")) s.drop(1).dropRight(1) else s // strip one redundant outer pair
  }
}
