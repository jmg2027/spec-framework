package streamproc.design.modules

import streamproc.hdl._
import framework.macros.localSpec
import framework.macros.Formal.*
import streamproc.design._
import streamproc.specs.modules.IngressBufferSpecs.*

class IngressBuffer(c: SPConfig) extends Module:
  localSpec(contIngressBuffer)
  val in = Flipped(Decoupled(new StreamBeat(c)))
  localSpec(intfIngressIn)
  val out = Decoupled(new StreamBeat(c))
  localSpec(intfIngressOut)

  localSpec(funcElasticBuffer)
  assert(assertProperty(propNoBeatLoss) { !(in.fire && !out.ready) }, "ingress dropped an accepted beat")
