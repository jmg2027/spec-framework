package framework.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import framework.spec.{MetaFile, HardwareSpecification}

object SpecEmit {
  /** Wrap a Spec DSL expression; emit `.spec` during compilation. */
  def emitSpec(body: HardwareSpecification): HardwareSpecification = macro impl

  def impl(c: blackbox.Context)(body: c.Expr[HardwareSpecification]): c.Expr[HardwareSpecification] = {
    import c.universe._

    // 1. Evaluate the builder expression inside the macro universe
    val spec: HardwareSpecification = c.eval(c.Expr[HardwareSpecification](c.untypecheck(body.tree.duplicate)))

    // 2. Write .spec file at compile‑time
    MetaFile.writeSpec(spec)

    // 3. Return the original expression unchanged so runtime semantics identical
    body
  }
}
