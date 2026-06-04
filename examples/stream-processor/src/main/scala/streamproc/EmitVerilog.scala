package streamproc

import circt.stage.ChiselStage
import streamproc.design._
import streamproc.design.top.StreamProcessorTop

/** Emit SystemVerilog (via firtool) so the real tools — verilator (sim) and
  * yosys/sby (formal) — can run against the design and its embedded assertions. */
object EmitVerilog {
  def main(args: Array[String]): Unit = {
    val c   = SPConfig(injectShaperBug = args.contains("--inject-bug"))
    val dir = "target/sv"
    java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dir))
    ChiselStage.emitSystemVerilogFile(
      new StreamProcessorTop(c),
      args        = Array("--split-verilog", "--target-dir", dir),
      firtoolOpts = Array("-enable-layers=Verification", "-strip-debug-info"),
    )
    println(s"[stream-processor] SystemVerilog (assertions+covers active, split) → $dir/")
  }
}
