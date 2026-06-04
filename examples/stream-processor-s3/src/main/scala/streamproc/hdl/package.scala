// A tiny Chisel-shaped HDL shim for Scala 3. Chisel is published only for Scala
// 2.13, so the Scala 3 counterpart of the design models the same structure
// (Module / Bundle / Decoupled ports) on this shim. The spec framework — which
// is what we are demonstrating on Scala 3 — sees the same bundles, ports and
// properties as the real Chisel build next door.
package streamproc

package object hdl {
  sealed trait Data
  final case class UInt(width: Int) extends Data
  final case class Bool() extends Data

  // Boolean-condition algebra so assertProperty conditions read like Chisel ones.
  implicit class BoolOps(val b: Bool) extends AnyVal {
    def &&(o: Bool): Bool   = Bool()
    def ||(o: Bool): Bool   = Bool()
    def unary_! : Bool      = Bool()
    def ===(o: Bool): Bool  = Bool()
  }
  implicit class UIntOps(val u: UInt) extends AnyVal {
    def <=(o: UInt): Bool  = Bool()
    def <(o: UInt): Bool   = Bool()
    def ===(o: UInt): Bool = Bool()
    def =/=(o: UInt): Bool = Bool()
  }

  class Bundle
  class Decoupled[T](val bits: T) extends Bundle {
    val valid = Bool()
    val ready = Bool()
    def fire: Bool = Bool()
  }
  def Decoupled[T](bits: T): Decoupled[T] = new Decoupled(bits)
  def Flipped[T](x: T): T = x

  class Module {
    def assert(cond: Bool, msg: String): Unit = ()
  }
}
