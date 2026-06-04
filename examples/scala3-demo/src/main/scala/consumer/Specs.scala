// the spec graph — by-value relations with forward references (Scala 3)
package consumer

import framework.macros.SpecEmit.spec
import framework.macros.TypedSpec.bundle
import framework.spec.Spec.*

object Specs:
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
