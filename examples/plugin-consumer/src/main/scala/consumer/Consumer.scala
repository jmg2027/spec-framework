// examples/plugin-consumer/.../consumer/Consumer.scala
// -----------------------------------------------------------------------------
//  A real single-module CONSUMER of the published framework that drives the
//  sbt plugin end to end (`sbt exportSpecIndex`). It exercises every feature
//  through the plugin's aggregation path: typed bundles, by-value relations
//  (so the plugin must resolve `@fqn:` references), @LocalSpec tags, and a bound
//  property (so properties.sva is produced).
//
//  Specs are declared above the design in the same file, so each `.spec` is
//  emitted (at compile time) before the design's `@LocalSpec` macros resolve it.
// -----------------------------------------------------------------------------
package consumer

import framework.macros.SpecEmit.spec
import framework.macros.TypedSpec.bundle
import framework.macros.LocalSpec
import framework.macros.Formal._
import framework.spec.Spec._

// ---- tiny hardware shim + implementation types ------------------------------
sealed trait Data
final case class UInt(width: Int) extends Data
final case class Bool() extends Data
class Bundle
class Module {
  def assert(c: Boolean, m: String): Unit = if (!c) System.err.println(s"[assert] $m")
}

class MemReq extends Bundle {
  val addr  = UInt(32)
  val txnId = UInt(2)
}

// ---- spec graph (by-value relations, forward refs) --------------------------
object Specs {
  val contMem = spec {
    CONTRACT("CONT_MEM_UNIT").desc("memory unit")
      .has(intfReqOut)            // by-value, forward reference
      .uses(paramAddrW)
      .build()
  }
  val intfReqOut = spec {
    INTERFACE("INTF_REQ_OUT").desc("request output").has(bndMemReq).build()
  }
  val paramAddrW = spec {
    PARAMETER("PARAM_ADDR_W").desc("address width").entry("default", "32").build()
  }
  val bndMemReq =
    bundle[MemReq]("BND_MEM_REQ", "memory request", "PARAM_ADDR_W")(_.addr, _.txnId)

  val propBounded = spec {
    PROPERTY("PROP_TXN_BOUNDED").desc("outstanding txns stay within table").build()
  }
}

// ---- design, anchored to the specs ------------------------------------------
import Specs._

@LocalSpec(contMem)
class MemUnit extends Module {
  @LocalSpec(intfReqOut)
  val reqOut = new MemReq

  var outstanding = 0
  val maxOutstanding = 4

  val bounded = assertProperty(propBounded) { outstanding <= maxOutstanding }
  assert(bounded, "txn table overflow")
}
