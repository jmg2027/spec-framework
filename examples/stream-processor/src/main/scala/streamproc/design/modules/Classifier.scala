package streamproc.design.modules

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.design._
import streamproc.specs.modules.ClassifierSpecs._

/** Table-driven classifier: matches the packet's TDEST at SOP and latches the
  * resulting action for the whole packet. */
class Classifier(c: SPConfig) extends Module {
  localSpec(contClassifier)

  val in = IO(Flipped(Decoupled(new TaggedBeat(c))))
  localSpec(intfClassifierIn)
  val out = IO(Decoupled(new TaggedBeat(c)))
  localSpec(intfClassifierOut)

  // Rule table (defaults to pass-through; a real design would expose a program port).
  val keys = RegInit(VecInit(Seq.tabulate(c.numRules)(i => i.U(c.destWidth.W))))
  val acts = RegInit(VecInit(Seq.fill(c.numRules)(0.U.asTypeOf(new RuleAction(c)))))

  localSpec(funcClassify)
  val matchVec = keys.map(_ === in.bits.beat.dest)
  val hit      = matchVec.reduce(_ || _)
  val hitIdx   = PriorityEncoder(matchVec)
  val lookedUp = Mux(hit, acts(hitIdx), 0.U.asTypeOf(new RuleAction(c)))

  val curAction = RegInit(0.U.asTypeOf(new RuleAction(c)))
  when(in.fire && in.bits.meta.sop) { curAction := lookedUp }
  val act = Mux(in.bits.meta.sop, lookedUp, curAction)

  out.valid          := in.valid
  in.ready           := out.ready
  out.bits           := in.bits
  out.bits.meta.drop := in.bits.meta.drop || act.drop
  out.bits.meta.prio := act.prio
  when(act.setDest) { out.bits.beat.dest := act.newDest }

  // The action applied to a non-SOP beat is exactly the one latched at SOP.
  assert(assertProperty(propActionStable) {
    !(out.fire && !in.bits.meta.sop) || (act.asUInt === curAction.asUInt)
  }, "[PROP_ACTION_STABLE] classifier action changed mid-packet")
}
