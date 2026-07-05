// Package-level `localSpec` so design code can `import framework.macros.localSpec`.
package framework

package object macros {
  import scala.language.experimental.macros
  import framework.spec.HardwareSpecification

  /** Anchor the enclosing declaration to `spec` (method form of @LocalSpec). */
  def localSpec(spec: HardwareSpecification): Unit = macro LocalSpecMethod.impl

  /** Value form: anchor `decl` (e.g. a port) to `spec` and return it, so the tag
    * is attached to that exact declaration rather than the enclosing module:
    *   `val in = localSpec(intfIn, IO(Flipped(Decoupled(...))))` */
  def localSpec[T](spec: HardwareSpecification, decl: T): T = macro LocalSpecMethod.implValue[T]
}
