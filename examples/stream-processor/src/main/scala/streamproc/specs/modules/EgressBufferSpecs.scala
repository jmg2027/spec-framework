package streamproc.specs.modules

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.bundles.BundleSpecs._

object EgressBufferSpecs {
  val intfEgressIn = spec {
    INTERFACE("INTF_EGRESS_IN").desc("Tagged beats in (carrying the drop/redirect verdict)")
      .entry("direction", "Flipped(Decoupled(TaggedBeat))").has(bndTaggedBeat).build()
  }
  val intfEgressOut = spec {
    INTERFACE("INTF_EGRESS_OUT").desc("Final AXI-Stream output")
      .entry("direction", "Decoupled(StreamBeat)").has(bndStreamBeat).build()
  }
  val funcDropFilter = spec {
    FUNCTION("FUNC_DROP_FILTER")
      .desc("Discard beats of packets marked drop; apply the redirected TDEST; buffer the rest to the output")
      .has(intfEgressIn, intfEgressOut)
      .uses(paramEgressDepth).build()
  }
  val propDroppedNotEmitted = spec {
    PROPERTY("PROP_DROPPED_NOT_EMITTED").desc("A beat whose packet was marked drop never appears on the output").build()
  }
  val covPacketDrop = spec {
    COVERAGE("COV_PACKET_DROP").desc("At least one packet is dropped by the classifier verdict").build()
  }
  val contEgressBuffer = spec {
    CONTRACT("CONT_EGRESS_BUFFER").desc("Applies the classifier verdict and buffers the egress stream")
      .has(intfEgressIn, intfEgressOut)
      .has(funcDropFilter)
      .uses(paramDataBytes, paramEgressDepth).build()
  }
}
