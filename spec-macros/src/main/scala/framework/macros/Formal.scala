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
    // Pretty-print the bound condition. The typed tree qualifies every field with
    // its synthetic `EnclosingModule.this.` prefix; strip that noise so the index
    // records the condition the way the engineer wrote it.
    val exprText = {
      val printed = try showCode(c.untypecheck(cond.tree.duplicate)) catch { case _: Throwable => showCode(cond.tree) }
      printed.replaceAll("""\b[A-Za-z_][A-Za-z0-9_]*\.this\.""", "").trim
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
    c.Expr[Boolean](q"${cond.tree}")
  }
}
