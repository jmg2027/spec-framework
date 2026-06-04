package streamproc.design.modules

import streamproc.hdl._
import framework.macros.localSpec
import framework.macros.Formal.*
import streamproc.design._
import streamproc.specs.modules.FramerSpecs.*

class Framer(c: SPConfig) extends Module:
  localSpec(contFramer)
  val in = Flipped(Decoupled(new StreamBeat(c)))
  localSpec(intfFramerIn)
  val out = Decoupled(new TaggedBeat(c))
  localSpec(intfFramerOut)

  localSpec(funcFraming)
  assert(assertProperty(propSopEopBalanced) { !(out.fire && out.bits.meta.eop) || out.bits.meta.sop }, "EOP outside an open packet")
