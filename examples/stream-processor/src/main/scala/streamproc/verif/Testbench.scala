package streamproc.verif

import framework.spec.Bench
import streamproc.design.SPConfig
// The SAME spec objects the RTL binds with assertProperty/coverProperty are
// reused here as the checkers / cover points — the spec is the single source of
// the verification plan.
import streamproc.specs.modules.IngressBufferSpecs.{propNoBeatLoss, covBackpressure}
import streamproc.specs.modules.FramerSpecs.propSopEopBalanced
import streamproc.specs.modules.ClassifierSpecs.propActionStable
import streamproc.specs.modules.ShaperSpecs.{propTokenBounded, covTokensDrained}
import streamproc.specs.modules.EgressBufferSpecs.{propDroppedNotEmitted, covPacketDrop}

/** Spec-driven simulation: drives stimulus through the pipeline model and checks
  * every PROPERTY / exercises every COVERAGE spec, writing VerifIndex.json that
  * SpecCheck folds back into the compliance report.
  *
  *   runMain streamproc.verif.Testbench              (all properties pass)
  *   runMain streamproc.verif.Testbench --inject-bug (PROP_TOKEN_BOUNDED fails)
  */
object Testbench {
  def main(args: Array[String]): Unit = {
    val bug   = args.contains("--inject-bug")
    val c     = SPConfig()
    val m     = new PipelineModel(c, injectBug = bug)
    val bench = new Bench
    val rng   = new scala.util.Random(1)

    bench.run(4000) { _ =>
      val in = if (rng.nextInt(100) < 75) Some(m.Beat(rng.nextInt(256), rng.nextInt(100) < 30, rng.nextInt(16))) else None
      val downstreamReady = rng.nextInt(100) < 55
      m.tick(in, downstreamReady)

      bench.checkProperty(propNoBeatLoss)       { !m.ingressDropped }
      bench.checkProperty(propTokenBounded)     { m.tokensNow <= c.shaperBurst }
      bench.checkProperty(propSopEopBalanced)   { m.framingOk }
      bench.checkProperty(propDroppedNotEmitted){ !m.emittedDrop }
      bench.checkProperty(propActionStable)     { m.actionStable }

      bench.coverPoint(covBackpressure)  { m.ingressFull }
      bench.coverPoint(covPacketDrop)    { m.packetDropped }
      bench.coverPoint(covTokensDrained) { m.tokensNow == 0 }
    }

    bench.writeIndex()
    val failed = bench.all.count(_.status == "failed")
    println(s"[testbench] ${bench.cycle} cycles, ${bench.all.size} specs exercised, $failed failing${if (bug) " (bug injected)" else ""}")
  }
}
