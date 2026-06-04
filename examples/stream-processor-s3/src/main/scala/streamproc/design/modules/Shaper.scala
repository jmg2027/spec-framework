package streamproc.design.modules

import streamproc.hdl._
import framework.macros.localSpec
import framework.macros.Formal.*
import streamproc.design._
import streamproc.specs.modules.ShaperSpecs.*

class Shaper(c: SPConfig) extends Module:
  localSpec(contShaper)
  val in = Flipped(Decoupled(new TaggedBeat(c)))
  localSpec(intfShaperIn)
  val out = Decoupled(new TaggedBeat(c))
  localSpec(intfShaperOut)

  val tokens = UInt(8)
  localSpec(funcRateLimit)
  assert(assertProperty(propTokenBounded) { tokens <= UInt(c.shaperBurst) }, "shaper token count exceeded burst")
