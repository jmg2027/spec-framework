package streamproc.design.modules

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.design._
import streamproc.specs.modules.EgressBufferSpecs._

/** Applies the classifier verdict (drop / redirect) and buffers the output. */
class EgressBuffer(c: SPConfig) extends Module {
  localSpec(contEgressBuffer)

  val in = IO(Flipped(Decoupled(new TaggedBeat(c))))
  localSpec(intfEgressIn)
  val out = IO(Decoupled(new StreamBeat(c)))
  localSpec(intfEgressOut)

  val q = Module(new Queue(new StreamBeat(c), c.egressDepth))
  localSpec(funcDropFilter)

  val dropping = in.bits.meta.drop
  q.io.enq.valid := in.valid && !dropping
  q.io.enq.bits  := in.bits.beat
  // accept (and discard) dropped beats so they do not block upstream
  in.ready := Mux(dropping, true.B, q.io.enq.ready)
  out <> q.io.deq

  assert(assertProperty(propDroppedNotEmitted) { !(q.io.enq.fire && in.bits.meta.drop) }, "[PROP_DROPPED_NOT_EMITTED] a dropped beat was enqueued")
  cover(coverProperty(covPacketDrop) { in.fire && in.bits.meta.drop }, "a packet was dropped")
}
