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
   *   Spec.CONTRACT(...).capability(...).build())
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

    // Evaluate the builder expression at compile time
    val spec: HardwareSpecification = try {
      c.eval(c.Expr[HardwareSpecification](c.untypecheck(body.tree.duplicate)))
    } catch {
      case e: Throwable =>
        c.abort(c.enclosingPosition,
          s"spec macro failed to evaluate the HardwareSpecification at compile time: ${e.getMessage}")
    }

    val specWithPath = spec.copy(scalaDeclarationPath = fqn)
    MetaFile.writeSpec(specWithPath)

    val fqnLit = Literal(Constant(fqn))
    c.Expr[HardwareSpecification](q"{ val _s = $body; _s.copy(scalaDeclarationPath = $fqnLit) }")
  }
}
