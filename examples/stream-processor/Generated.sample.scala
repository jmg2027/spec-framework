// GENERATED skeleton from the spec graph by SpecGen. Fill in the logic.
package generated

import chisel3._
import chisel3.util._
import framework.macros.localSpec
import framework.macros.Formal._
import streamproc.specs.bundles.BundleSpecs._
import streamproc.specs.modules.ClassifierSpecs._
import streamproc.specs.modules.EgressBufferSpecs._
import streamproc.specs.modules.FramerSpecs._
import streamproc.specs.modules.IngressBufferSpecs._
import streamproc.specs.modules.ShaperSpecs._
import streamproc.specs.params.ParamSpecs._
import streamproc.specs.top.TopSpecs._

final case class Config(
  dataBytes: Int = 4,  // PARAM_DATA_BYTES
  destWidth: Int = 4,  // PARAM_DEST_WIDTH
  egressDepth: Int = 8,  // PARAM_EGRESS_DEPTH
  headerBytes: Int = 8,  // PARAM_HEADER_BYTES
  idWidth: Int = 4,  // PARAM_ID_WIDTH
  ingressDepth: Int = 8,  // PARAM_INGRESS_DEPTH
  numRules: Int = 8,  // PARAM_NUM_RULES
  shaperBurst: Int = 16,  // PARAM_SHAPER_BURST
)

/** Per-beat metadata derived by the pipeline (framing + classifier verdict) (BND_PACKET_META) */
class PacketMeta(c: Config) extends Bundle {
  val sop = Bool()
  val eop = Bool()
  val drop = Bool()
  val prio = UInt(2.W)
}

/** Classifier verdict: drop / redirect / set priority (BND_RULE_ACTION) */
class RuleAction(c: Config) extends Bundle {
  val drop = Bool()
  val setDest = Bool()
  val newDest = UInt(4.W)
  val prio = UInt(2.W)
}

/** AXI-Stream beat: data + byte-enables + end-of-packet framing + routing tags (BND_STREAM_BEAT) */
class StreamBeat(c: Config) extends Bundle {
  val data = UInt(32.W)
  val keep = UInt(4.W)
  val last = Bool()
  val id = UInt(4.W)
  val dest = UInt(4.W)
}

/** A beat paired with its metadata — the unit that flows between stages (BND_TAGGED_BEAT) */
class TaggedBeat(c: Config) extends Bundle {
  val beat = new StreamBeat(c)
  val meta = new PacketMeta(c)
}

/** Table-driven packet classifier producing a per-packet action (CONT_CLASSIFIER) */
class Classifier(c: Config) extends Module {
  localSpec(contClassifier)

  val classifierIn = localSpec(intfClassifierIn, IO(Flipped(Decoupled(new TaggedBeat(c)))))
  val classifierOut = localSpec(intfClassifierOut, IO(Decoupled(new TaggedBeat(c))))

  localSpec(funcClassify)  // On SOP, match the packet's TDEST against the rule table and latch the 

  // ---- TODO: replace DontCare with real logic ----
  classifierIn.ready := DontCare
  classifierOut.valid := DontCare; classifierOut.bits := DontCare

  assert(assertProperty(propActionStable) { true.B /* TODO: The action chosen at SOP is applied identically to ever */ }, "PROP_ACTION_STABLE")
}

/** Applies the classifier verdict and buffers the egress stream (CONT_EGRESS_BUFFER) */
class EgressBuffer(c: Config) extends Module {
  localSpec(contEgressBuffer)

  val egressIn = localSpec(intfEgressIn, IO(Flipped(Decoupled(new TaggedBeat(c)))))
  val egressOut = localSpec(intfEgressOut, IO(Decoupled(new StreamBeat(c))))

  localSpec(funcDropFilter)  // Discard beats of packets marked drop; apply the redirected TDEST; buff

  // ---- TODO: replace DontCare with real logic ----
  egressIn.ready := DontCare
  egressOut.valid := DontCare; egressOut.bits := DontCare

  cover(coverProperty(covPacketDrop) { false.B /* TODO */ }, "COV_PACKET_DROP")
  assert(assertProperty(propDroppedNotEmitted) { true.B /* TODO: A beat whose packet was marked drop never appears on th */ }, "PROP_DROPPED_NOT_EMITTED")
}

/** Annotates the stream with packet framing metadata (CONT_FRAMER) */
class Framer(c: Config) extends Module {
  localSpec(contFramer)

  val framerIn = localSpec(intfFramerIn, IO(Flipped(Decoupled(new StreamBeat(c)))))
  val framerOut = localSpec(intfFramerOut, IO(Decoupled(new TaggedBeat(c))))

  localSpec(funcFraming)  // Derive start-of-packet / end-of-packet from TLAST and attach it as met

  // ---- TODO: replace DontCare with real logic ----
  framerIn.ready := DontCare
  framerOut.valid := DontCare; framerOut.bits := DontCare

  assert(assertProperty(propSopEopBalanced) { true.B /* TODO: A beat is never marked EOP outside an open packet */ }, "PROP_SOP_EOP_BALANCED")
}

/** Per-input elastic FIFO providing backpressure and rate decoupling (CONT_INGRESS_BUFFER) */
class IngressBuffer(c: Config) extends Module {
  localSpec(contIngressBuffer)

  val ingressIn = localSpec(intfIngressIn, IO(Flipped(Decoupled(new StreamBeat(c)))))
  val ingressOut = localSpec(intfIngressOut, IO(Decoupled(new StreamBeat(c))))

  localSpec(funcElasticBuffer)  // Absorb bursts and decouple upstream/downstream rates via a FIFO; never

  // ---- TODO: replace DontCare with real logic ----
  ingressIn.ready := DontCare
  ingressOut.valid := DontCare; ingressOut.bits := DontCare

  cover(coverProperty(covBackpressure) { false.B /* TODO */ }, "COV_BACKPRESSURE")
  assert(assertProperty(propNoBeatLoss) { true.B /* TODO: Every accepted input beat is enqueued (no silent drop u */ }, "PROP_NO_BEAT_LOSS")
}

/** Optional token-bucket rate shaper (instantiated when enableShaper = true) (CONT_SHAPER) */
class Shaper(c: Config) extends Module {
  localSpec(contShaper)

  val shaperIn = localSpec(intfShaperIn, IO(Flipped(Decoupled(new TaggedBeat(c)))))
  val shaperOut = localSpec(intfShaperOut, IO(Decoupled(new TaggedBeat(c))))

  localSpec(funcRateLimit)  // Token-bucket shaper: refill one token per cycle up to the burst size; 

  // ---- TODO: replace DontCare with real logic ----
  shaperIn.ready := DontCare
  shaperOut.valid := DontCare; shaperOut.bits := DontCare

  assert(assertProperty(propTokenBounded) { true.B /* TODO: The token count never exceeds the configured burst size */ }, "PROP_TOKEN_BOUNDED")
  cover(coverProperty(covTokensDrained) { false.B /* TODO */ }, "COV_TOKENS_DRAINED")
}

/** Configurable AXI-Stream packet-processing pipeline (top) (CONT_STREAM_PROCESSOR) */
class StreamProcessor(c: Config) extends Module {
  localSpec(contStreamProcessor)

  val streamIn = localSpec(intfStreamIn, IO(Flipped(Decoupled(new StreamBeat(c)))))
  val streamOut = localSpec(intfStreamOut, IO(Decoupled(new StreamBeat(c))))

  val classifier = Module(new Classifier(c))
  val egressBuffer = Module(new EgressBuffer(c))
  val framer = Module(new Framer(c))
  val ingressBuffer = Module(new IngressBuffer(c))
  val shaper = Module(new Shaper(c))

  localSpec(funcPipeline)  // | Wire the stages into a single Decoupled pipeline: | ingress → framer

  // ---- TODO: replace DontCare with real logic ----
  streamIn.ready := DontCare
  streamOut.valid := DontCare; streamOut.bits := DontCare
  classifier.classifierIn.valid := DontCare; classifier.classifierIn.bits := DontCare
  classifier.classifierOut.ready := DontCare
  egressBuffer.egressIn.valid := DontCare; egressBuffer.egressIn.bits := DontCare
  egressBuffer.egressOut.ready := DontCare
  framer.framerIn.valid := DontCare; framer.framerIn.bits := DontCare
  framer.framerOut.ready := DontCare
  ingressBuffer.ingressIn.valid := DontCare; ingressBuffer.ingressIn.bits := DontCare
  ingressBuffer.ingressOut.ready := DontCare
  shaper.shaperIn.valid := DontCare; shaper.shaperIn.bits := DontCare
  shaper.shaperOut.ready := DontCare


}

