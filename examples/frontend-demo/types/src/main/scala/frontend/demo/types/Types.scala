// examples/frontend-demo/types/.../frontend/demo/types/Types.scala
// -----------------------------------------------------------------------------
//  Implementation types for the demo fetch-unit (mirrors a slice of a real
//  RISC-V frontend). These are the SINGLE SOURCE OF TRUTH for bundle shape:
//  the typed specs in the `specs` module bind to these classes, so any change
//  here that the spec does not follow becomes a compile error in the spec.
// -----------------------------------------------------------------------------
package frontend.demo.types

import minichisel._

/** Frontend parameters. */
final case class Params(pcWidth: Int = 32, txnIdWidth: Int = 1, fetchBytes: Int = 4)

/** External program-memory fetch request. */
class FetchRequest(p: Params) extends Bundle {
  val addr  = UInt(p.pcWidth)
  val txnId = UInt(p.txnIdWidth)
}

/** External program-memory fetch response. */
class FetchResponse(p: Params) extends Bundle {
  val data  = UInt(p.fetchBytes * 8)
  val txnId = UInt(p.txnIdWidth)
  val eccOK = Bool()
}

/** A single issued instruction slot. */
class InstrSlot(p: Params) extends Bundle {
  val instruction = UInt(32)
  val pc          = UInt(p.pcWidth)
  val valid       = Bool()
}
