package streamproc.design.modules

import streamproc.hdl._
import framework.macros.localSpec
import framework.macros.Formal.*
import streamproc.design._
import streamproc.specs.modules.EgressBufferSpecs.*

class EgressBuffer(c: SPConfig) extends Module:
  localSpec(contEgressBuffer)
  val in = Flipped(Decoupled(new TaggedBeat(c)))
  localSpec(intfEgressIn)
  val out = Decoupled(new StreamBeat(c))
  localSpec(intfEgressOut)

  localSpec(funcDropFilter)
  assert(assertProperty(propDroppedNotEmitted) { !(out.fire && in.bits.meta.drop) }, "a dropped beat was emitted")
