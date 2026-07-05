// spec-macros/src/main/scala/framework/macros/SpecEmit.scala
// Macro utility for compile-time emission of hardware specification metadata.
// Used to wrap Spec DSL expressions and emit .spec files during compilation.
package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import framework.spec.{HardwareSpecification, MetaFile}

/**
 * SpecEmit: Macro utility for emitting hardware specification metadata at
 * compile time.
 *
 * Usage: val mySpec = spec {
 *   Spec.CONTRACT(...).desc("...").entry(...).build()
 * }
 *
 * This macro will:
 *   1. Evaluate the builder expression at compile time (macro JVM), 2. Write
 *      the resulting HardwareSpecification as a .spec file (via MetaFile), 3.
 *      Return the original HardwareSpecification expression so runtime
 *      semantics are unchanged.
 *
 * This enables compile-time emission of metadata for all hardware specs, which
 * are later aggregated by the SBT plugin into JSON indices for documentation,
 * analysis, or tooling.
 */
object SpecEmit {

  /**
   * Wrap a Spec DSL expression; emit `.spec` during compilation.
   *
   * @param body
   *   HardwareSpecification builder expression (e.g.
   *   Spec.CONTRACT(...).desc(...).entry(...).build())
   * @return
   *   The same HardwareSpecification as the input, but with a .spec file
   *   emitted at compile time.
   */
  def spec(body: HardwareSpecification): HardwareSpecification = macro impl

  /**
   * Macro implementation for spec.
   *
   *   1. Evaluates the builder expression at compile time (in the macro JVM).
   *      2. Writes the resulting HardwareSpecification as a .spec file using
   *      MetaFile.writeSpec. 3. Returns the original HardwareSpecification
   *      expression so runtime semantics are unchanged.
   *
   * @param c
   *   Macro context (blackbox)
   * @param body
   *   The builder expression to evaluate (should yield a HardwareSpecification)
   * @return
   *   The original builder expression, unchanged (for runtime use)
   */
  def impl(
    c: blackbox.Context,
  )(body: c.Expr[HardwareSpecification]): c.Expr[HardwareSpecification] = {
    import c.universe._
    val fqn = c.internal.enclosingOwner.fullName

    // -------------------------------------------------------------------------
    // Relation references by VALUE.
    //
    // The builder is evaluated at compile time (below), so a relation argument
    // that is a sibling spec `val` — `.has(intfFoo)` — cannot be dereferenced:
    // its enclosing object is still being compiled. We therefore rewrite every
    // relation argument that is a *stable reference* to a `HardwareSpecification`
    // into a `@fqn:<fullName>` string literal, BEFORE evaluation. Three things
    // follow: the eval only ever sees strings (no NPE / no forced ordering),
    // the reference is still type-checked (a typo'd name is a compile error, so
    // the compiler guarantees the referenced spec exists), and the relation is
    // recorded by the referent's declaration path — which the aggregator
    // (SpecCheck) resolves back to its id. Plain string-id relations are left
    // untouched, so existing code keeps working.
    // -------------------------------------------------------------------------
    val hwType = typeOf[HardwareSpecification]
    def isStableSpecRef(a: Tree): Boolean =
      a.tpe != null && a.tpe <:< hwType &&
        a.symbol != null && a.symbol.isTerm && a.symbol.asTerm.isStable
    val rewriter = new Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case Apply(fun, args) =>
          val newArgs = args.map { a =>
            if (isStableSpecRef(a)) Literal(Constant("@fqn:" + a.symbol.fullName))
            else transform(a)
          }
          treeCopy.Apply(tree, transform(fun), newArgs)
        case _ => super.transform(tree)
      }
    }
    val rewritten = c.untypecheck(rewriter.transform(body.tree.duplicate))

    // Evaluate the (rewritten) builder expression at compile time
    val spec: HardwareSpecification = try {
      c.eval(c.Expr[HardwareSpecification](rewritten.duplicate))
    } catch {
      case e: Throwable =>
        c.abort(c.enclosingPosition,
          s"spec macro failed to evaluate the HardwareSpecification at compile time: ${e.getMessage}")
    }

    val specWithPath = spec.copy(scalaDeclarationPath = fqn)
    MetaFile.writeSpec(specWithPath)

    val fqnLit = Literal(Constant(fqn))
    // Runtime value uses the same rewritten body, so it never dereferences a
    // sibling val either (keeps forward references safe at runtime too).
    c.Expr[HardwareSpecification](q"{ val _s = $rewritten; _s.copy(scalaDeclarationPath = $fqnLit) }")
  }
}
