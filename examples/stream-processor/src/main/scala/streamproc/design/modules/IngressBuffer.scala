package streamproc.design.modules

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.design._
import streamproc.specs.modules.IngressBufferSpecs._

/** Per-input elastic FIFO: absorbs bursts and provides backpressure. */
class IngressBuffer(c: SPConfig) extends Module {
  localSpec(contIngressBuffer)

  val in = localSpec(intfIngressIn, IO(Flipped(Decoupled(new StreamBeat(c)))))
  val out = localSpec(intfIngressOut, IO(Decoupled(new StreamBeat(c))))

  val q = Module(new Queue(new StreamBeat(c), c.ingressDepth))
  localSpec(funcElasticBuffer)
  q.io.enq <> in
  out <> q.io.deq

  assert(assertProperty(propNoBeatLoss) { !(in.fire && !q.io.enq.fire) }, "[PROP_NO_BEAT_LOSS] ingress dropped an accepted beat")
  cover(coverProperty(covBackpressure) { in.valid && !in.ready }, "ingress back-pressured")
}
