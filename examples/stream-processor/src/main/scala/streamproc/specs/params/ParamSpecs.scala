package streamproc.specs.params

import framework.macros.TypedSpec.paramSpec
import streamproc.design.SPConfig

/** PARAMETER specs bound to the config case class: the field name/type come from
  * the selector and the default is read from `SPConfig()` — so the spec and the
  * implementation can no longer drift (one source of truth for parameters). */
object ParamSpecs {
  private val d = SPConfig()

  val paramDataBytes =
    paramSpec[SPConfig]("PARAM_DATA_BYTES", "Stream datapath width in bytes (power of two)", d)(_.dataBytes)
  val paramIdWidth =
    paramSpec[SPConfig]("PARAM_ID_WIDTH", "AXI-Stream TID width", d)(_.idWidth)
  val paramDestWidth =
    paramSpec[SPConfig]("PARAM_DEST_WIDTH", "AXI-Stream TDEST width (selects the output port)", d)(_.destWidth)
  val paramHeaderBytes =
    paramSpec[SPConfig]("PARAM_HEADER_BYTES", "Bytes captured into the packet header for classification", d)(_.headerBytes)
  val paramNumRules =
    paramSpec[SPConfig]("PARAM_NUM_RULES", "Classifier table depth (number of match rules)", d)(_.numRules)
  val paramIngressDepth =
    paramSpec[SPConfig]("PARAM_INGRESS_DEPTH", "Ingress elastic FIFO depth", d)(_.ingressDepth)
  val paramEgressDepth =
    paramSpec[SPConfig]("PARAM_EGRESS_DEPTH", "Egress FIFO depth", d)(_.egressDepth)
  val paramShaperBurst =
    paramSpec[SPConfig]("PARAM_SHAPER_BURST", "Token-bucket burst size for the rate shaper", d)(_.shaperBurst)
}
