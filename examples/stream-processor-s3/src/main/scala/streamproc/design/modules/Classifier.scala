package streamproc.design.modules

import streamproc.hdl._
import framework.macros.localSpec
import framework.macros.Formal.*
import streamproc.design._
import streamproc.specs.modules.ClassifierSpecs.*

class Classifier(c: SPConfig) extends Module:
  localSpec(contClassifier)
  val in = Flipped(Decoupled(new TaggedBeat(c)))
  localSpec(intfClassifierIn)
  val out = Decoupled(new TaggedBeat(c))
  localSpec(intfClassifierOut)

  localSpec(funcClassify)
  assert(assertProperty(propActionStable) { !(out.fire && !in.bits.meta.sop) || in.bits.meta.drop === out.bits.meta.drop }, "action changed mid-packet")
