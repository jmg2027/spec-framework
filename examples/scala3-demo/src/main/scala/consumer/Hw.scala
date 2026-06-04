// hardware shim + implementation types (no macros)
package consumer

sealed trait Data
final case class UInt(width: Int) extends Data
final case class Bool() extends Data
class Bundle
class Module:
  def assert(c: Boolean, m: String): Unit = if !c then System.err.println(s"[assert] $m")

class MemReq extends Bundle:
  val addr  = UInt(32)
  val txnId = UInt(2)
