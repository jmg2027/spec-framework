package streamproc.specs.modules

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.bundles.BundleSpecs._

object FramerSpecs {
  val intfFramerIn = spec {
    INTERFACE("INTF_FRAMER_IN").desc("Raw beats in")
      .entry("direction", "Flipped(Decoupled(StreamBeat))")
      .has(bndStreamBeat).build()
  }
  val intfFramerOut = spec {
    INTERFACE("INTF_FRAMER_OUT").desc("Tagged beats out (beat + framing metadata)")
      .entry("direction", "Decoupled(TaggedBeat)")
      .has(bndTaggedBeat).build()
  }
  val funcFraming = spec {
    FUNCTION("FUNC_FRAMING")
      .desc("Derive start-of-packet / end-of-packet from TLAST and attach it as metadata to each beat")
      .has(intfFramerIn, intfFramerOut)
      .note("SOP = first beat after reset or after a TLAST; EOP = beat with TLAST asserted").build()
  }
  val propSopEopBalanced = spec {
    PROPERTY("PROP_SOP_EOP_BALANCED").desc("A beat is never marked EOP outside an open packet").build()
  }
  val contFramer = spec {
    CONTRACT("CONT_FRAMER").desc("Annotates the stream with packet framing metadata")
      .has(intfFramerIn, intfFramerOut)
      .has(funcFraming).build()
  }
}
