# Enhanced Builder Pattern Usage Examples

The enhanced builder pattern provides type-safe and user-friendly methods for creating tables, diagrams, and code snippets in your hardware specifications.

## Table Building

### 1. Using the Fluent Table Builder

```scala
val spec = CONTRACT("MEMORY_SPEC").desc("Memory interface specification")
  .table("Memory Interface Signals") { builder =>
    builder
      .withHeaders("Signal", "Width", "Direction", "Description")
      .addRow("clk", "1", "Input", "Clock signal")
      .addRow("rst", "1", "Input", "Reset signal")  
      .addRow("addr", "32", "Input", "Address bus")
      .addRow("data_in", "64", "Input", "Write data")
      .addRow("data_out", "64", "Output", "Read data")
      .addRow("we", "1", "Input", "Write enable")
  }
  .build()
```

### 2. Using Simple Table Builder

```scala
val spec = INTERFACE("BUS_SPEC").desc("Bus protocol specification")
  .simpleTable("Phase", "Duration", "Action")(
    List("Setup", "2 cycles", "Address valid"),
    List("Access", "1 cycle", "Data transfer"),
    List("Hold", "1 cycle", "Data hold")
  )
  .build()
```

### 3. Pre-built Table Objects

```scala
val memoryTable = SpecTable("Register Map")
  .withHeaders("Address", "Register", "Access", "Description")
  .addRow("0x0000", "CTRL", "RW", "Control register")
  .addRow("0x0004", "STATUS", "RO", "Status register")
  .addRow("0x0008", "DATA", "RW", "Data register")
  .build()

val spec = PARAMETER("REG_SPEC").desc("Register specification")
  .table(memoryTable)
  .build()
```

## Drawing/Diagram Building

### 1. Mermaid Diagrams

```scala
val spec = FUNCTION("FSM_SPEC").desc("Finite state machine specification")
  .mermaidDiagram("State Machine", "graph TD",
    """
    IDLE --> ACTIVE: start
    ACTIVE --> PROCESSING: data_valid
    PROCESSING --> DONE: complete
    DONE --> IDLE: reset
    """)
  .build()
```

### 2. ASCII Diagrams

```scala
val spec = INTERFACE("PIPELINE_SPEC").desc("Pipeline stage specification")
  .asciiDiagram("Pipeline Structure",
    """
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ Stage 1 │───▶│ Stage 2 │───▶│ Stage 3 │
    │ Fetch   │    │ Decode  │    │ Execute │
    └─────────┘    └─────────┘    └─────────┘
    """)
  .build()
```

### 3. SVG Diagrams

```scala
val spec = CAPABILITY("TIMING_SPEC").desc("Timing diagram specification")
  .svgDiagram("Clock and Data Timing",
    """<svg width="400" height="200">
      <rect x="0" y="0" width="400" height="200" fill="white" stroke="black"/>
      <text x="200" y="100" text-anchor="middle">Timing Diagram</text>
    </svg>""")
  .build()
```

### 4. External Image References

```scala
val spec = PROPERTY("ARCH_SPEC").desc("Architecture specification")
  .imageRef("System Architecture", "docs/images/system_arch.png", "System architecture diagram")
  .build()
```

## Code Building

### 1. Language-specific Code Builders

```scala
val spec = FUNCTION("COUNTER_SPEC").desc("Counter implementation specification")
  .chiselCode("Counter Implementation",
    """class Counter(width: Int) extends Module {
      val io = IO(new Bundle {
        val en = Input(Bool())
        val count = Output(UInt(width.W))
      })
      
      val reg = RegInit(0.U(width.W))
      when(io.en) {
        reg := reg + 1.U
      }
      io.count := reg
    }""",
    "Chisel implementation of a simple counter")
  .verilogCode("Verilog Equivalent",
    """module counter #(parameter WIDTH = 8) (
      input clk,
      input rst,
      input en,
      output reg [WIDTH-1:0] count
    );
      always @(posedge clk) begin
        if (rst) count <= 0;
        else if (en) count <= count + 1;
      end
    endmodule""",
    "Equivalent Verilog implementation")
  .build()
```

### 2. Generic Code Builder

```scala
val jsonConfig = SpecCode.json(
  """{"clock_frequency": 100000000, "data_width": 32}""",
  "Configuration",
  "JSON configuration for the module"
)

val spec = PARAMETER("CONFIG_SPEC").desc("Configuration specification")
  .code(jsonConfig)
  .build()
```

## Combined Example

```scala
val comprehensiveSpec = CONTRACT("DMA_CONTROLLER").desc("DMA Controller specification")
  .is("BUS_INTERFACE", "MEMORY_CONTROLLER")
  .has("DESCRIPTOR_MANAGER")
  .status("DRAFT")
  .entry("Version", "1.0")
  .entry("Author", "Hardware Team")
  
  // Configuration table
  .table("DMA Configuration Parameters") { builder =>
    builder
      .withHeaders("Parameter", "Value", "Description")
      .addRow("Max Channels", "8", "Maximum number of DMA channels")
      .addRow("Buffer Size", "4KB", "Internal buffer size per channel")
      .addRow("Address Width", "32 bits", "System address bus width")
  }
  
  // State machine diagram
  .mermaidDiagram("DMA State Machine", "stateDiagram-v2",
    """
    [*] --> Idle
    Idle --> Setup: configure
    Setup --> Transfer: start
    Transfer --> Transfer: data_valid
    Transfer --> Complete: done
    Complete --> Idle: ack
    """)
    
  // Chisel implementation
  .chiselCode("DMA Controller Interface",
    """class DMAController extends Module {
      val io = IO(new Bundle {
        val config = Input(new DMAConfig)
        val bus = new AXI4Bundle
        val status = Output(new DMAStatus)
      })
      // Implementation details...
    }""",
    "Main DMA controller module interface")
    
  .note("This specification covers the high-level architecture and interface")
  .build()
```

## Benefits of the Enhanced Builder Pattern

1. **Type Safety**: Compile-time validation of table structure and diagram types
2. **Rich API**: Fluent builders for complex structures
3. **Language Support**: Built-in support for common hardware languages
4. **Backward Compatibility**: Legacy string methods still available (deprecated)
5. **Extensibility**: Easy to add new diagram types or code languages
6. **Validation**: Automatic validation of table consistency (column counts, etc.)
7. **Markdown Generation**: Automatic conversion to markdown for documentation

## Migration from Legacy API

Old code using string-based methods:
```scala
.table("|Signal|Width|\n|clk|1|\n|data|32|")
.draw("ASCII diagram here")
.code("```scala\ncode here\n```")
```

New type-safe equivalent:
```scala
.simpleTable("Signal", "Width")(List("clk", "1"), List("data", "32"))
.asciiDiagram("Block Diagram", "ASCII diagram here")  
.scalaCode("Implementation", "code here")
```
