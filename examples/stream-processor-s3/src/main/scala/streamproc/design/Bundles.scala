package streamproc.design

import streamproc.hdl._

/** Same config + bundles as the Chisel build, on the Scala 3 shim. The field
  * names match so the shared typed bundle specs (`bundle[StreamBeat](_.data,…)`)
  * resolve identically. */
final case class SPConfig(
    dataBytes: Int = 4,
    idWidth: Int = 4,
    destWidth: Int = 4,
    headerBytes: Int = 8,
    numRules: Int = 8,
    ingressDepth: Int = 8,
    egressDepth: Int = 8,
    enableShaper: Boolean = true,
    shaperBurst: Int = 16,
) {
  def dataWidth: Int = dataBytes * 8
}

class StreamBeat(c: SPConfig) extends Bundle {
  val data = UInt(c.dataWidth)
  val keep = UInt(c.dataBytes)
  val last = Bool()
  val id   = UInt(c.idWidth)
  val dest = UInt(c.destWidth)
}

class PacketMeta(c: SPConfig) extends Bundle {
  val sop  = Bool()
  val eop  = Bool()
  val drop = Bool()
  val prio = UInt(2)
}

class TaggedBeat(c: SPConfig) extends Bundle {
  val beat = new StreamBeat(c)
  val meta = new PacketMeta(c)
}

class RuleAction(c: SPConfig) extends Bundle {
  val drop    = Bool()
  val setDest = Bool()
  val newDest = UInt(c.destWidth)
  val prio    = UInt(2)
}
