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
 * Usage: val mySpec = emitSpec {
 * Spec.CONTRACT(...).capability(...).entry(...).build() }
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
  def emitSpec(body: HardwareSpecification): HardwareSpecification = macro impl

  /**
   * Macro implementation for emitSpec.
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
    // 1. Evaluate the builder expression at compile time (macro JVM)
    val spec: HardwareSpecification = try {
      c.eval(c.Expr[HardwareSpecification](c.untypecheck(body.tree.duplicate)))
    } catch {
      case e: Throwable =>
        c.abort(c.enclosingPosition, s"emitSpec macro failed to evaluate the HardwareSpecification at compile time: ${e.getMessage}")
    }
    // 2. Write the .spec file at compile time using MetaFile
    MetaFile.writeSpec(spec)
    // 3. Return the original builder expression unchanged (for runtime semantics)
    body
  }
}
