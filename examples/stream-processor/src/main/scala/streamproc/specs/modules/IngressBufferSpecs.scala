package streamproc.specs.modules

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.bundles.BundleSpecs._

object IngressBufferSpecs {
  val intfIngressIn = spec {
    INTERFACE("INTF_INGRESS_IN").desc("Ingress AXI-Stream input (Decoupled)")
      .entry("direction", "Flipped(Decoupled(StreamBeat))")
      .has(bndStreamBeat).build()
  }
  val intfIngressOut = spec {
    INTERFACE("INTF_INGRESS_OUT").desc("Buffered AXI-Stream output (Decoupled)")
      .entry("direction", "Decoupled(StreamBeat)")
      .has(bndStreamBeat).build()
  }
  val funcElasticBuffer = spec {
    FUNCTION("FUNC_ELASTIC_BUFFER")
      .desc("Absorb bursts and decouple upstream/downstream rates via a FIFO; never drop an accepted beat")
      .has(intfIngressIn, intfIngressOut)
      .uses(paramIngressDepth).build()
  }
  val propNoBeatLoss = spec {
    PROPERTY("PROP_NO_BEAT_LOSS").desc("Every accepted input beat is enqueued (no silent drop under backpressure)").build()
  }
  val covBackpressure = spec {
    COVERAGE("COV_BACKPRESSURE").desc("The ingress FIFO fills and back-pressures upstream at least once").build()
  }
  val contIngressBuffer = spec {
    CONTRACT("CONT_INGRESS_BUFFER").desc("Per-input elastic FIFO providing backpressure and rate decoupling")
      .has(intfIngressIn, intfIngressOut)
      .has(funcElasticBuffer)
      .uses(paramDataBytes, paramIngressDepth).build()
  }
}
