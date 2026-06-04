package streamproc.verif

import streamproc.design.SPConfig
import scala.collection.mutable

/** A pure-Scala cycle model of the pipeline whose observable invariants ARE the
  * PROPERTY/COVERAGE specs. (No simulator is available in this environment; with
  * verilator the same checkers would peek the real DUT instead of this model.)
  *
  * `injectBug = true` breaks the shaper's token cap, so PROP_TOKEN_BOUNDED fails
  * — demonstrating the spec-driven testbench catching a regression. */
final class PipelineModel(c: SPConfig, injectBug: Boolean = false) {
  final case class Beat(data: Int, last: Boolean, dest: Int)

  private val ingress = mutable.Queue.empty[Beat]
  private val egress  = mutable.Queue.empty[Beat]
  private var inPacket   = false
  private var tokens     = c.shaperBurst
  private var dropPkt    = false
  private var lastAction = -1

  // observable signals, refreshed each tick
  var ingressDropped = false
  var ingressFull    = false
  var framingOk      = true
  var emittedDrop    = false
  var actionStable   = true
  var packetDropped  = false
  def tokensNow: Int = tokens

  /** Classifier rule: TDEST ≡ 3 (mod 4) ⇒ drop the packet. */
  private def classify(dest: Int): Boolean = (dest % 4) == 3

  def tick(in: Option[Beat], downstreamReady: Boolean): Unit = {
    ingressDropped = false; framingOk = true; emittedDrop = false
    actionStable = true; packetDropped = false

    // ingress FIFO (lossless: when full, upstream is back-pressured, never dropped)
    ingressFull = ingress.size >= c.ingressDepth
    in.foreach(b => if (!ingressFull) ingress.enqueue(b))

    // shaper token refill (one per cycle, capped at burst — unless the bug is on)
    if (tokens < c.shaperBurst) tokens += 1
    else if (injectBug) tokens += 1 // BUG: refill past the burst size

    val canForward = ingress.nonEmpty && tokens > 0 && downstreamReady
    if (canForward) {
      val b   = ingress.dequeue()
      val sop = !inPacket
      val eop = b.last
      framingOk = !(eop && !inPacket && !sop) // EOP only inside an open packet (or a 1-beat SOP)
      inPacket  = !b.last

      if (sop) { dropPkt = classify(b.dest); if (dropPkt) packetDropped = true }
      val act = if (dropPkt) 1 else 0
      if (!sop) actionStable = act == lastAction
      lastAction = act

      tokens -= 1
      if (dropPkt) emittedDrop = false else egress.enqueue(b)
    }
    if (downstreamReady && egress.nonEmpty) egress.dequeue()
  }
}
