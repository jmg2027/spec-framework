// examples/frontend-demo/specs/.../frontend/demo/specs/FetchSpecs.scala
// -----------------------------------------------------------------------------
//  The spec graph for the demo fetch-unit.
//
//  Two things to notice:
//   • BUNDLE specs use the new TYPED builder `bundleSpec[T]`. The field selectors
//     (`_.addr`, `_.txnId`, …) are checked against the real implementation types
//     in `frontend.demo.types`. Rename a field there and THIS file stops
//     compiling — field drift is a compile error.
//   • Relations (`is/has/uses`) reference other nodes by string id. This is the
//     framework's convention: the `spec { … }` macro evaluates each spec at
//     compile time, so it cannot dereference sibling vals in the same module —
//     and string ids are exactly what the checker validates for dangling refs.
//   • Two nodes are left deliberately "loose" to show the checker earning its
//     keep: `bndInstrSlot` omits the `valid` field (incomplete bundle) and
//     `propEpochClean` is declared but never bound to an assertion (unenforced).
// -----------------------------------------------------------------------------
package frontend.demo.specs

import framework.macros.SpecEmit.spec
import framework.macros.TypedSpec.{bundle, bundleLenient}
import framework.spec.Spec._
import frontend.demo.types._

object FetchSpecs {

  // ---- Parameters ---------------------------------------------------------
  val paramPcWidth = spec {
    PARAMETER("PARAM_PC_WIDTH").desc("Program counter width in bits")
      .entry("default", "32").build()
  }
  val paramTxnId = spec {
    PARAMETER("PARAM_TXNID_WIDTH").desc("Transaction id width for outstanding fetches")
      .entry("default", "1").build()
  }

  // ---- Typed bundles (the "타입화" feature) --------------------------------
  // `bundle[T]` is a macro: it reads each field's NAME and TYPE straight from the
  // selector tree (`_.addr` ⇒ "addr" : UInt) and emits the `.spec` at compile
  // time. The field selectors are checked against T's actual members — rename a
  // field in `frontend.demo.types` and this file stops compiling. Completeness is
  // strict by default: every public field must be declared, or it is a compile
  // error (use `bundleLenient` to downgrade that to a warning).
  // Signature: bundle[T](id, desc, usesParamIds*)(fieldSelectors*)
  val bndFetchReq =
    bundle[FetchRequest]("BND_FETCH_REQUEST", "EPM fetch request",
      "PARAM_PC_WIDTH", "PARAM_TXNID_WIDTH")(_.addr, _.txnId)

  val bndFetchResp =
    bundle[FetchResponse]("BND_FETCH_RESPONSE", "EPM fetch response",
      "PARAM_TXNID_WIDTH")(_.data, _.txnId, _.eccOK)

  // INTENTIONALLY incomplete: `valid` is left undeclared. `bundleLenient` turns
  // that into a compile WARNING (+ checker note) instead of an error.
  val bndInstrSlot =
    bundleLenient[InstrSlot]("BND_INSTR_SLOT", "Issued instruction slot",
      "PARAM_PC_WIDTH")(_.instruction, _.pc)

  // ---- Interfaces ---------------------------------------------------------
  val intfEpmReqOut = spec {
    INTERFACE("INTF_EPM_REQ_OUT").desc("Fetch request output to external program memory")
      .has("BND_FETCH_REQUEST").build()
  }
  val intfEpmRespIn = spec {
    INTERFACE("INTF_EPM_RESP_IN").desc("Fetch response input from external program memory")
      .has("BND_FETCH_RESPONSE").build()
  }

  // ---- Functions ----------------------------------------------------------
  val funcFetchRequest = spec {
    FUNCTION("FUNC_FETCH_REQUEST").desc("Generate aligned fetch requests, allocate txn entry")
      .has("INTF_EPM_REQ_OUT").build()
  }
  val funcFetchResponse = spec {
    FUNCTION("FUNC_FETCH_RESPONSE").desc("Match response txn, forward data, deallocate entry")
      .has("INTF_EPM_RESP_IN").build()
  }

  // ---- Contract -----------------------------------------------------------
  val contFetchUnit = spec {
    CONTRACT("CONT_FETCH_UNIT").desc("Fetch unit: issues memory requests and tracks responses")
      .has("INTF_EPM_REQ_OUT", "INTF_EPM_RESP_IN")
      .has("FUNC_FETCH_REQUEST", "FUNC_FETCH_RESPONSE")
      .uses("PARAM_PC_WIDTH", "PARAM_TXNID_WIDTH")
      .build()
  }

  // ---- Properties & coverage ---------------------------------------------
  val propNoOverflow = spec {
    PROPERTY("PROP_NO_REQTABLE_OVERFLOW").desc("Outstanding requests never exceed table size").build()
  }
  val propInOrderResp = spec {
    PROPERTY("PROP_IN_ORDER_RESPONSE").desc("Responses are consumed in request order").build()
  }
  // INTENTIONALLY unenforced: declared but never bound to a check → checker warns.
  val propEpochClean = spec {
    PROPERTY("PROP_EPOCH_FLUSH_CLEAN").desc("An epoch flush clears every outstanding entry").build()
  }
  val covBackpressure = spec {
    COVERAGE("COV_OUTPUT_BACKPRESSURE").desc("The output is back-pressured at least once").build()
  }
}
