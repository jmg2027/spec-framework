package streamproc.design

import chisel3._

/** AXI-Stream beat: payload + byte-enables + end-of-packet framing + routing tags. */
class StreamBeat(c: SPConfig) extends Bundle {
  val data = UInt(c.dataWidth.W)
  val keep = UInt(c.dataBytes.W)
  val last = Bool()
  val id   = UInt(c.idWidth.W)
  val dest = UInt(c.destWidth.W)
}

/** Per-beat metadata the pipeline derives (framing, classifier verdict). */
class PacketMeta(c: SPConfig) extends Bundle {
  val sop  = Bool()
  val eop  = Bool()
  val drop = Bool()
  val prio = UInt(2.W)
}

/** A beat paired with its metadata — the unit that flows between stages. */
class TaggedBeat(c: SPConfig) extends Bundle {
  val beat = new StreamBeat(c)
  val meta = new PacketMeta(c)
}

/** Classifier verdict for a packet. */
class RuleAction(c: SPConfig) extends Bundle {
  val drop    = Bool()
  val setDest = Bool()
  val newDest = UInt(c.destWidth.W)
  val prio    = UInt(2.W)
}
