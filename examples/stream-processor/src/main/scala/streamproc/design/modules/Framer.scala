package streamproc.design.modules

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.design._
import streamproc.specs.modules.FramerSpecs._

/** Derives SOP/EOP framing from TLAST and attaches it as per-beat metadata. */
class Framer(c: SPConfig) extends Module {
  localSpec(contFramer)

  val in = IO(Flipped(Decoupled(new StreamBeat(c))))
  localSpec(intfFramerIn)
  val out = IO(Decoupled(new TaggedBeat(c)))
  localSpec(intfFramerOut)

  val inPacket = RegInit(false.B)
  localSpec(funcFraming)
  when(in.fire) { inPacket := !in.bits.last }

  out.valid         := in.valid
  in.ready          := out.ready
  out.bits.beat     := in.bits
  out.bits.meta.sop := !inPacket
  out.bits.meta.eop := in.bits.last
  out.bits.meta.drop := false.B
  out.bits.meta.prio := 0.U

  // EOP is only ever produced inside an open packet (or for a single-beat packet, which is also SOP).
  assert(assertProperty(propSopEopBalanced) {
    !(out.fire && out.bits.meta.eop) || inPacket || out.bits.meta.sop
  }, "EOP asserted outside an open packet")
}
