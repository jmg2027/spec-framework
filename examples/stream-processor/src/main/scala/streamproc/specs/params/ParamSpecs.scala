package streamproc.specs.params

import framework.macros.SpecEmit.spec
import framework.spec.Spec._

/** PARAMETER specs — the single source of truth for the design's knobs. */
object ParamSpecs {
  val paramDataBytes =
    spec { PARAMETER("PARAM_DATA_BYTES").desc("Stream datapath width in bytes (power of two)").entry("default", "4").build() }
  val paramIdWidth =
    spec { PARAMETER("PARAM_ID_WIDTH").desc("AXI-Stream TID width").entry("default", "4").build() }
  val paramDestWidth =
    spec { PARAMETER("PARAM_DEST_WIDTH").desc("AXI-Stream TDEST width (selects the output port)").entry("default", "4").build() }
  val paramHeaderBytes =
    spec { PARAMETER("PARAM_HEADER_BYTES").desc("Bytes captured into the packet header for classification").entry("default", "8").build() }
  val paramNumRules =
    spec { PARAMETER("PARAM_NUM_RULES").desc("Classifier table depth (number of match rules)").entry("default", "8").build() }
  val paramIngressDepth =
    spec { PARAMETER("PARAM_INGRESS_DEPTH").desc("Ingress elastic FIFO depth").entry("default", "8").build() }
  val paramEgressDepth =
    spec { PARAMETER("PARAM_EGRESS_DEPTH").desc("Egress FIFO depth").entry("default", "8").build() }
  val paramShaperBurst =
    spec { PARAMETER("PARAM_SHAPER_BURST").desc("Token-bucket burst size for the rate shaper").entry("default", "16").build() }
}
