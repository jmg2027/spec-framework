package streamproc.specs.bundles

import framework.macros.TypedSpec.bundle
import streamproc.design._

/** Typed BUNDLE specs bound to the Chisel bundles — field names/types are read
  * from the implementation, so renaming a field in `Bundles.scala` breaks this. */
object BundleSpecs {
  val bndStreamBeat =
    bundle[StreamBeat]("BND_STREAM_BEAT",
      "AXI-Stream beat: data + byte-enables + end-of-packet framing + routing tags",
      "PARAM_DATA_BYTES", "PARAM_ID_WIDTH", "PARAM_DEST_WIDTH")(_.data, _.keep, _.last, _.id, _.dest)

  val bndPacketMeta =
    bundle[PacketMeta]("BND_PACKET_META",
      "Per-beat metadata derived by the pipeline (framing + classifier verdict)")(_.sop, _.eop, _.drop, _.prio)

  val bndTaggedBeat =
    bundle[TaggedBeat]("BND_TAGGED_BEAT",
      "A beat paired with its metadata — the unit that flows between stages")(_.beat, _.meta)

  val bndRuleAction =
    bundle[RuleAction]("BND_RULE_ACTION",
      "Classifier verdict: drop / redirect / set priority",
      "PARAM_DEST_WIDTH")(_.drop, _.setDest, _.newDest, _.prio)
}
