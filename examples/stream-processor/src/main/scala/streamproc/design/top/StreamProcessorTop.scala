package streamproc.design.top

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import streamproc.design._
import streamproc.design.modules._
import streamproc.specs.top.TopSpecs._

/** Configurable AXI-Stream packet-processing pipeline:
  *   ingress → framer → classifier → (shaper?) → egress
  */
class StreamProcessorTop(c: SPConfig) extends Module {
  localSpec(contStreamProcessor)

  val in = IO(Flipped(Decoupled(new StreamBeat(c))))
  localSpec(intfStreamIn)
  val out = IO(Decoupled(new StreamBeat(c)))
  localSpec(intfStreamOut)

  val ingress    = Module(new IngressBuffer(c))
  val framer     = Module(new Framer(c))
  val classifier = Module(new Classifier(c))
  val egress     = Module(new EgressBuffer(c))

  localSpec(funcPipeline)
  ingress.in    <> in
  framer.in     <> ingress.out
  classifier.in <> framer.out

  if (c.enableShaper) {
    val shaper = Module(new Shaper(c))
    shaper.in <> classifier.out
    egress.in <> shaper.out
  } else {
    egress.in <> classifier.out
  }
  out <> egress.out
}
