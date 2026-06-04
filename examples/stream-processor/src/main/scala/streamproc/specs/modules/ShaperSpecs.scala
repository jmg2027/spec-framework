package streamproc.specs.modules

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.bundles.BundleSpecs._

object ShaperSpecs {
  val intfShaperIn = spec {
    INTERFACE("INTF_SHAPER_IN").desc("Tagged beats in")
      .entry("direction", "Flipped(Decoupled(TaggedBeat))").has(bndTaggedBeat).build()
  }
  val intfShaperOut = spec {
    INTERFACE("INTF_SHAPER_OUT").desc("Rate-limited beats out")
      .entry("direction", "Decoupled(TaggedBeat)").has(bndTaggedBeat).build()
  }
  val funcRateLimit = spec {
    FUNCTION("FUNC_RATE_LIMIT")
      .desc("Token-bucket shaper: refill one token per cycle up to the burst size; each forwarded beat consumes a token")
      .has(intfShaperIn, intfShaperOut)
      .uses(paramShaperBurst).build()
  }
  val propTokenBounded = spec {
    PROPERTY("PROP_TOKEN_BOUNDED").desc("The token count never exceeds the configured burst size").build()
  }
  val contShaper = spec {
    CONTRACT("CONT_SHAPER").desc("Optional token-bucket rate shaper (instantiated when enableShaper = true)")
      .has(intfShaperIn, intfShaperOut)
      .has(funcRateLimit)
      .uses(paramShaperBurst)
      .note("Configurable: omitted entirely from the pipeline when disabled").build()
  }
}
