// examples/frontend-demo/types/.../minichisel/MiniChisel.scala
// -----------------------------------------------------------------------------
//  A *tiny* Chisel-shaped shim so the demo is self-contained and fast to build.
//  It is deliberately not Chisel — but the spec-framework features exercised here
//  (typed `bundleSpec[T]`, `@LocalSpec`, `assertProperty`) map 1:1 onto real
//  chisel3 types: `Bundle` ↔ chisel3.Bundle, `UInt/Bool` ↔ chisel3.UInt/Bool,
//  `assert/cover` ↔ chisel3.assert/cover. Swap this package for `import chisel3._`
//  and the spec/design code is unchanged.
// -----------------------------------------------------------------------------
package minichisel

sealed trait Data
final case class UInt(width: Int) extends Data
final case class Bool() extends Data

/** Base type for hardware bundles; fields are declared as public `val`s. */
class Bundle

/** Base type for hardware modules. */
class Module {
  def assert(cond: Boolean, msg: String): Unit =
    if (!cond) System.err.println(s"[assert FAILED] $msg")
  def cover(cond: Boolean, msg: String): Unit = ()
}
