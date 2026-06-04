// examples/frontend-demo/specs/.../frontend/demo/specs/FetchSpecs.scala
// -----------------------------------------------------------------------------
//  The spec graph for the demo fetch-unit.
//
//  Relations reference other nodes BY VALUE (`.has(intfEpmReqOut)`), not by
//  string id. The `spec { … }` macro rewrites those references to the referent's
//  declaration path before evaluating, so:
//   • a typo'd name is a COMPILE ERROR — the compiler guarantees the referenced
//     spec exists (try `./run-demo.sh --ref-drift`);
//   • FORWARD references work — `contFetchUnit` below is declared *first* and
//     references interfaces/functions/params defined later in the file (the
//     pattern that used to throw an opaque compile-time `null`).
//  The checker resolves those references back to ids and still flags any that do
//  not resolve.
//
//  Typed BUNDLE specs use `bundle[T]` (compile-time, field selectors checked
//  against the implementation type). `bndInstrSlot` uses `bundleLenient` to leave
//  `valid` undeclared on purpose; `propEpochClean` is left unbound on purpose.
// -----------------------------------------------------------------------------
package frontend.demo.specs

import framework.macros.SpecEmit.spec
import framework.macros.TypedSpec.{bundle, bundleLenient}
import framework.spec.Spec._
import frontend.demo.types._

object FetchSpecs {

  // ---- Contract (declared FIRST; forward-references everything below) ------
  val contFetchUnit = spec {
    CONTRACT("CONT_FETCH_UNIT").desc("Fetch unit: issues memory requests and tracks responses")
      .has(intfEpmReqOut, intfEpmRespIn)
      .has(funcFetchRequest, funcFetchResponse)
      .uses(paramPcWidth, paramTxnId)
      .build()
  }

  // ---- Interfaces ---------------------------------------------------------
  val intfEpmReqOut = spec {
    INTERFACE("INTF_EPM_REQ_OUT").desc("Fetch request output to external program memory")
      .has(bndFetchReq).build()
  }
  val intfEpmRespIn = spec {
    INTERFACE("INTF_EPM_RESP_IN").desc("Fetch response input from external program memory")
      .has(bndFetchResp).build()
  }

  // ---- Functions ----------------------------------------------------------
  val funcFetchRequest = spec {
    FUNCTION("FUNC_FETCH_REQUEST").desc("Generate aligned fetch requests, allocate txn entry")
      .has(intfEpmReqOut).build()
  }
  val funcFetchResponse = spec {
    FUNCTION("FUNC_FETCH_RESPONSE").desc("Match response txn, forward data, deallocate entry")
      .has(intfEpmRespIn).build()
  }

  // ---- Parameters ---------------------------------------------------------
  val paramPcWidth = spec {
    PARAMETER("PARAM_PC_WIDTH").desc("Program counter width in bits").entry("default", "32").build()
  }
  val paramTxnId = spec {
    PARAMETER("PARAM_TXNID_WIDTH").desc("Transaction id width for outstanding fetches").entry("default", "1").build()
  }

  // ---- Typed bundles ------------------------------------------------------
  // (`bundle[T]` reads field name + type from the selector; `uses` takes param ids.)
  val bndFetchReq =
    bundle[FetchRequest]("BND_FETCH_REQUEST", "EPM fetch request",
      "PARAM_PC_WIDTH", "PARAM_TXNID_WIDTH")(_.addr, _.txnId)

  val bndFetchResp =
    bundle[FetchResponse]("BND_FETCH_RESPONSE", "EPM fetch response",
      "PARAM_TXNID_WIDTH")(_.data, _.txnId, _.eccOK)

  // INTENTIONALLY incomplete: `valid` undeclared → compile warning + checker note.
  val bndInstrSlot =
    bundleLenient[InstrSlot]("BND_INSTR_SLOT", "Issued instruction slot",
      "PARAM_PC_WIDTH")(_.instruction, _.pc)

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
