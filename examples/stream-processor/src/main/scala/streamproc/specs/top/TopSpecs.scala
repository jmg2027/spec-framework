package streamproc.specs.top

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.bundles.BundleSpecs._
import streamproc.specs.modules.IngressBufferSpecs._
import streamproc.specs.modules.FramerSpecs._
import streamproc.specs.modules.ClassifierSpecs._
import streamproc.specs.modules.ShaperSpecs._
import streamproc.specs.modules.EgressBufferSpecs._

object TopSpecs {
  val intfStreamIn = spec {
    INTERFACE("INTF_STREAM_IN").desc("Top-level AXI-Stream input")
      .entry("direction", "Flipped(Decoupled(StreamBeat))").has(bndStreamBeat).build()
  }
  val intfStreamOut = spec {
    INTERFACE("INTF_STREAM_OUT").desc("Top-level AXI-Stream output")
      .entry("direction", "Decoupled(StreamBeat)").has(bndStreamBeat).build()
  }
  val funcPipeline = spec {
    FUNCTION("FUNC_PIPELINE")
      .desc("""
        | Wire the stages into a single Decoupled pipeline:
        |   ingress → framer → classifier → (shaper?) → egress
        | The shaper is included only when enableShaper = true; otherwise the
        | classifier connects straight to egress (configurable depth).
      """)
      .has(intfStreamIn, intfStreamOut)
      .note("Backpressure propagates end-to-end; lossless except where a packet is explicitly dropped").build()
  }
  val contStreamProcessor = spec {
    CONTRACT("CONT_STREAM_PROCESSOR")
      .desc("Configurable AXI-Stream packet-processing pipeline (top)")
      .has(intfStreamIn, intfStreamOut)
      .has(contIngressBuffer, contFramer, contClassifier, contShaper, contEgressBuffer)
      .has(funcPipeline)
      .uses(paramDataBytes, paramDestWidth, paramHeaderBytes, paramNumRules, paramShaperBurst)
      .note("Modular: each stage is its own contract; the shaper stage is optional.")
      .build()
  }
}
