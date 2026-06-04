package streamproc.design

/** Configuration for the stream processor. Drives datapath widths, FIFO depths,
  * classifier table size, and which optional stages are instantiated — so the
  * same source scales from a tiny config to a wide, deep one (configurability /
  * modularity / extensibility). */
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
  require(dataBytes >= 1 && (dataBytes & (dataBytes - 1)) == 0, "dataBytes must be a power of two")
  require(headerBytes >= dataBytes, "headerBytes must be >= dataBytes")
  require(numRules >= 1, "numRules must be >= 1")

  def dataWidth: Int   = dataBytes * 8
  def headerWidth: Int = headerBytes * 8
  def headerBeats: Int = (headerBytes + dataBytes - 1) / dataBytes
}
