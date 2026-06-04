package streamproc.specs.modules

import framework.macros.SpecEmit.spec
import framework.spec.Spec._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.bundles.BundleSpecs._

object ClassifierSpecs {
  val intfClassifierIn = spec {
    INTERFACE("INTF_CLASSIFIER_IN").desc("Tagged beats in")
      .entry("direction", "Flipped(Decoupled(TaggedBeat))").has(bndTaggedBeat).build()
  }
  val intfClassifierOut = spec {
    INTERFACE("INTF_CLASSIFIER_OUT").desc("Classified beats out (metadata updated with the verdict)")
      .entry("direction", "Decoupled(TaggedBeat)").has(bndTaggedBeat).build()
  }
  val funcClassify = spec {
    FUNCTION("FUNC_CLASSIFY")
      .desc("On SOP, match the packet's TDEST against the rule table and latch the resulting action for the whole packet")
      .has(intfClassifierIn, intfClassifierOut)
      .has(bndRuleAction)
      .uses(paramNumRules)
      .note("First-match wins; no match ⇒ pass-through (no drop, no redirect)").build()
  }
  val propActionStable = spec {
    PROPERTY("PROP_ACTION_STABLE").desc("The action chosen at SOP is applied identically to every beat of the packet").build()
  }
  val contClassifier = spec {
    CONTRACT("CONT_CLASSIFIER").desc("Table-driven packet classifier producing a per-packet action")
      .has(intfClassifierIn, intfClassifierOut)
      .has(funcClassify)
      .uses(paramNumRules, paramDestWidth).build()
  }
}
