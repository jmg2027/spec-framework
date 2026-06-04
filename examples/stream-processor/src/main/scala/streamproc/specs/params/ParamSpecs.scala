package streamproc.specs.params

import framework.macros.TypedSpec.param
import streamproc.design.SPConfig

/** PARAMETER specs, emitted at COMPILE time (build-time pure — this is the real
  * target: a production Chisel/ASIC build whose spec graph is a pure artefact of
  * `sbt compile`, materialised by the CI gate without any Chisel run).
  *
  * The selector (`_.dataBytes`) binds the name/type to the `SPConfig` field, so a
  * rename of the config field is a compile error here. The default is restated as
  * a compile-time literal (the trade-off for not needing a run). If you would
  * rather single-source the default from a live `SPConfig()` and can afford a
  * runtime emission pass, swap `param` for `paramSpec` (see `TypedSpec`). */
object ParamSpecs {
  val paramDataBytes =
    param[SPConfig]("PARAM_DATA_BYTES", "Stream datapath width in bytes (power of two)", 4)(_.dataBytes)
  val paramIdWidth =
    param[SPConfig]("PARAM_ID_WIDTH", "AXI-Stream TID width", 4)(_.idWidth)
  val paramDestWidth =
    param[SPConfig]("PARAM_DEST_WIDTH", "AXI-Stream TDEST width (selects the output port)", 4)(_.destWidth)
  val paramHeaderBytes =
    param[SPConfig]("PARAM_HEADER_BYTES", "Bytes captured into the packet header for classification", 8)(_.headerBytes)
  val paramNumRules =
    param[SPConfig]("PARAM_NUM_RULES", "Classifier table depth (number of match rules)", 8)(_.numRules)
  val paramIngressDepth =
    param[SPConfig]("PARAM_INGRESS_DEPTH", "Ingress elastic FIFO depth", 8)(_.ingressDepth)
  val paramEgressDepth =
    param[SPConfig]("PARAM_EGRESS_DEPTH", "Egress FIFO depth", 8)(_.egressDepth)
  val paramShaperBurst =
    param[SPConfig]("PARAM_SHAPER_BURST", "Token-bucket burst size for the rate shaper", 16)(_.shaperBurst)
}
