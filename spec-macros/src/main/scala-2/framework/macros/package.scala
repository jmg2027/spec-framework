// Package-level `localSpec` so `import framework.macros.localSpec` works the same
// on Scala 2 (this package object) and Scala 3 (a top-level def).
package framework

package object macros {
  import scala.language.experimental.macros
  import framework.spec.HardwareSpecification

  /** Anchor the enclosing declaration to `spec` (method form of @LocalSpec). */
  def localSpec(spec: HardwareSpecification): Unit = macro LocalSpecMethod.impl
}
