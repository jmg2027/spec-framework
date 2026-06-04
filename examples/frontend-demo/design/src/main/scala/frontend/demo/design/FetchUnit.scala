// examples/frontend-demo/design/.../frontend/demo/design/FetchUnit.scala
// -----------------------------------------------------------------------------
//  The RTL side of the demo. Every structural spec node is anchored to a concrete
//  declaration with `@LocalSpec`, and every enforced property is bound to a real
//  boolean check with `assertProperty` / `coverProperty`.
//
//  (Logic is modelled with plain elaboration-time values for brevity; in real
//   Chisel these would be hardware signals and the assert/cover would be
//   chisel3.assert/cover. The spec bindings are identical either way.)
// -----------------------------------------------------------------------------
package frontend.demo.design

import minichisel._
import framework.macros.LocalSpec
import framework.macros.Formal._
import frontend.demo.types._
import frontend.demo.specs.FetchSpecs._

@LocalSpec(contFetchUnit)
class FetchUnit(p: Params) extends Module {

  // -- Interfaces -----------------------------------------------------------
  @LocalSpec(intfEpmReqOut)
  val reqOut = new FetchRequest(p)

  @LocalSpec(intfEpmRespIn)
  val respIn = new FetchResponse(p)

  // -- Request-table model --------------------------------------------------
  val maxOutstanding = 1 << p.txnIdWidth
  var reqCount       = 0
  var respCount      = 0

  // -- Functions ------------------------------------------------------------
  @LocalSpec(funcFetchRequest)
  val fetchRequestLogic = {
    // would drive reqOut and allocate a txn entry; modelled as a counter bump
    reqCount += 1
  }

  @LocalSpec(funcFetchResponse)
  val fetchResponseLogic = {
    // would match respIn.txnId and forward data downstream
    respCount += 1
  }

  // -- Formal connections (the "formal 연결" feature) -----------------------
  // Each declared property is bound to an actual checkable condition. The macro
  // records property-id ⇐ condition-text ⇐ source-location into the index.
  val noOverflow = assertProperty(propNoOverflow) { (reqCount - respCount) <= maxOutstanding }
  val inOrder    = assertProperty(propInOrderResp) { respCount <= reqCount }
  val backPressure = coverProperty(covBackpressure) { reqCount > respCount }

  assert(noOverflow, "request table overflow")
  assert(inOrder, "out-of-order response consumed")
  cover(backPressure, "output backpressure observed")

  // NOTE: propEpochClean is intentionally NOT bound here → the checker reports it
  //       as an unenforced property.
}
