// StandaloneSpecExample.scala
// Example usage of the standalone spec framework

// Just copy StandaloneSpec.scala to your project and then use like this:
import framework.specs._

object ExampleSpecs {
  
  def main(args: Array[String]): Unit = {
    println("=== Standalone Spec Framework Example ===")
    
    cpuSpec
    memorySpec
    
    // Also test the module with specs
    new ExampleCPU()
    new ExampleMemory()
    
    // And the more specs
    MoreSpecs.busSpec
    
    println("=== All specs built successfully! ===")
  }
  
  val cpuSpec = Spec.CONTRACT("CPU_001")
    .desc("CPU core specification")
    .status("DRAFT")
    .entry("Author", "CPU Team")
    .entry("Version", "1.0")
    .table("markdown", """
| Register | Width | Purpose |
|----------|-------|---------|
| PC       | 32    | Program Counter |
| SP       | 32    | Stack Pointer |
| ACC      | 32    | Accumulator |
""")
    .draw("mermaid", """
graph TD
  A[Fetch] --> B[Decode]
  B --> C[Execute]
  C --> D[Writeback]
  D --> A
""")
    .code("scala", """
class CPU extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val data_out = Output(UInt(32.W))
  })
  
  val pc = RegInit(0.U(32.W))
  val acc = RegInit(0.U(32.W))
  
  // CPU implementation...
}
""")
    .note("This CPU implements a simple RISC architecture")
    .build()

  val memorySpec = Spec.INTERFACE("MEM_001")
    .desc("Memory interface specification")
    .entry("- Main requirements")
    .entry("  - Read/write operations")
    .entry("  - 32-bit addressing")
    .entry("  - Cache coherency")
    .entry("- Performance")
    .entry("  - Single cycle access")
    .entry("  - Pipeline support")
    .draw("ascii", """
    ┌─────────┐    ┌─────────┐
    │   CPU   │───▶│ Memory  │
    │         │◀───│         │
    └─────────┘    └─────────┘
    """)
    .build()
}

// Example of using the annotation (does nothing but compiles)
@LocalSpec(ExampleSpecs.cpuSpec)
class ExampleCPU {
  // Your actual hardware implementation
  println("CPU implementation here")
}

@LocalSpec(ExampleSpecs.memorySpec)  
class ExampleMemory {
  // Your actual memory implementation
  println("Memory implementation here")
}

// Example of using the spec macro (passthrough function)
object MoreSpecs {
  val busSpec = SpecEmit.emit {
    Spec.INTERFACE("BUS_001")
      .desc("System bus specification")
      .code("verilog", """
module system_bus (
  input clk,
  input rst,
  input [31:0] addr,
  inout [31:0] data
);
  // Bus implementation
endmodule
""")
      .build()
  }
}
