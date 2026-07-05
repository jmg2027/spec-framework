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

  // `cond` is generic so the same macro works for a Scala `Boolean` and a
  // `chisel3.Bool` (or any condition type). The condition is returned unchanged
  // for wiring into a real assert/cover.

  /** Bind PROPERTY `spec` to assertion `cond`; returns `cond` for wiring. */
  def assertProperty[T](spec: HardwareSpecification)(cond: T): T = macro implAssert[T]

  /** Bind COVERAGE `spec` to cover point `cond`; returns `cond` for wiring. */
  def coverProperty[T](spec: HardwareSpecification)(cond: T): T = macro implCover[T]

  def implAssert[T](c: blackbox.Context)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[T],
  ): c.Expr[T] = emit(c)("assert")(spec)(cond)

  def implCover[T](c: blackbox.Context)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[T],
  ): c.Expr[T] = emit(c)("cover")(spec)(cond)

  private def emit[T](c: blackbox.Context)(kind: String)(spec: c.Expr[HardwareSpecification])(
      cond: c.Expr[T],
  ): c.Expr[T] = {
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
    // Record the condition exactly as the engineer wrote it. The typechecked tree
    // is unreadable (Chisel desugars `a && b` into `a.do_$amp$amp(b)(implicit
    // SourceInfo)`), so we recover the verbatim source span from the ranged leaf
    // positions; only if that fails do we pretty-print the tree.
    val exprText = {
      val poss = scala.collection.mutable.ListBuffer.empty[c.universe.Position]
      (new c.universe.Traverser {
        override def traverse(t: c.universe.Tree): Unit = {
          if (t.pos != c.universe.NoPosition && t.pos.isRange) poss += t.pos
          super.traverse(t)
        }
      }).traverse(cond.tree)
      if (poss.nonEmpty) {
        val src   = poss.head.source
        val start = poss.map(_.start).min
        val end   = poss.map(_.end).max
        new String(src.content.slice(start, end)).trim
      } else renderInfix(c)(cond.tree)
    }

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
    c.Expr[T](q"${cond.tree}")
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
