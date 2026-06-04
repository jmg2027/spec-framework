package streamproc

import circt.stage.ChiselStage
import streamproc.design._
import streamproc.design.top.StreamProcessorTop

/** Emit SystemVerilog (via firtool) so the real tools — verilator (sim) and
  * yosys/sby (formal) — can run against the design and its embedded assertions. */
object EmitVerilog {
  def main(args: Array[String]): Unit = {
    val c   = SPConfig()
    val dir = "target/sv"
    val sv = ChiselStage.emitSystemVerilog(
      new StreamProcessorTop(c),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
    )
    java.nio.file.Files.createDirectories(java.nio.file.Paths.get(dir))
    java.nio.file.Files.write(java.nio.file.Paths.get(s"$dir/StreamProcessorTop.sv"), sv.getBytes)
    println(s"[stream-processor] SystemVerilog → $dir/StreamProcessorTop.sv (${sv.linesIterator.size} lines)")
  }
}
