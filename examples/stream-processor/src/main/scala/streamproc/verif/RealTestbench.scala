package streamproc.verif

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import framework.spec.Bench
import streamproc.design._
import streamproc.design.top.StreamProcessorTop
import streamproc.specs.modules.IngressBufferSpecs.{propNoBeatLoss, covBackpressure}
import streamproc.specs.modules.FramerSpecs.propSopEopBalanced
import streamproc.specs.modules.ClassifierSpecs.propActionStable
import streamproc.specs.modules.ShaperSpecs.propTokenBounded
import streamproc.specs.modules.EgressBufferSpecs.propDroppedNotEmitted

/** REAL verification backend: drives the elaborated DUT under **verilator**
  * (via ChiselSim). The spec assertions emitted by `assertProperty` are checked
  * by the simulator itself; a failure aborts the run and its `[PROP_*]` message
  * tells us which spec broke. Results are written with backend = "verilator".
  *
  *   runMain streamproc.verif.RealTestbench               (design holds → all pass)
  *   runMain streamproc.verif.RealTestbench --inject-bug  (token cap broken → PROP_TOKEN_BOUNDED fails)
  */
object RealTestbench {
  private val props = List(propNoBeatLoss, propSopEopBalanced, propActionStable, propTokenBounded, propDroppedNotEmitted)

  def main(args: Array[String]): Unit = {
    val bug    = args.contains("--inject-bug")
    val cycles = 3000
    val c      = SPConfig(injectShaperBug = bug)
    val bench  = new Bench("verilator")
    val rng    = new scala.util.Random(7)

    var ran        = 0
    var bpHits     = 0
    var failedId   = ""
    var failCycle  = -1

    try {
      simulate(new StreamProcessorTop(c)) { dut =>
        dut.reset.poke(true.B); dut.clock.step(2); dut.reset.poke(false.B)
        var i = 0
        while (i < cycles) {
          val vin = rng.nextInt(100) < 75
          dut.in.valid.poke(vin.B)
          dut.in.bits.data.poke(rng.nextInt(256).U)
          dut.in.bits.keep.poke(((1 << c.dataBytes) - 1).U)
          dut.in.bits.last.poke((rng.nextInt(100) < 30).B)
          dut.in.bits.id.poke(0.U)
          dut.in.bits.dest.poke(rng.nextInt(16).U)
          dut.out.ready.poke((rng.nextInt(100) < 55).B)

          if (vin && !dut.in.ready.peek().litToBoolean) bpHits += 1
          dut.clock.step()
          i += 1; ran = i
        }
      }
    } catch {
      case e: Throwable =>
        val msg = Option(e.getMessage).getOrElse("") + " " + e.toString
        failedId  = props.map(_.id).find(id => msg.contains(s"[$id]")).getOrElse("UNKNOWN")
        failCycle = ran
        System.err.println(s"[real-tb] assertion fired: $failedId at ~cycle $ran")
    }

    // map results back to the spec graph
    props.foreach { p =>
      if (p.id == failedId) bench.recordAssert(p, passed = false, atCycle = failCycle, cycles = ran)
      else                  bench.recordAssert(p, passed = true,  atCycle = -1,        cycles = ran)
    }
    bench.recordCover(covBackpressure, hits = bpHits, cycles = ran)

    bench.writeIndex()
    println(s"[real-tb] verilator ran $ran cycles; ${if (failedId.nonEmpty) s"FAILED: $failedId" else "all spec assertions held"}")
  }
}
