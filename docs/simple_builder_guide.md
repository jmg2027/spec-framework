# Simple Builder Pattern Improvements

The improved builder pattern provides natural, markdown-friendly methods for creating tables, diagrams, and code snippets in your hardware specifications.

## Hierarchical Lists

### Creating Nested Lists

```scala
val spec = FUNCTION("MODULE_SPEC").desc("Module specification")
  .listItem("Main Features", 0)
  .listItem("High Performance", 1)
  .listItem("Low Latency", 2)
  .listItem("High Throughput", 2)
  .listItem("Easy Integration", 1)
  .listItem("Standard Interfaces", 2)
  .listItem("Configurable Parameters", 2)
  .build()
```

This generates:
```markdown
- Main Features
  - High Performance
    - Low Latency
    - High Throughput
  - Easy Integration
    - Standard Interfaces
    - Configurable Parameters
```

## Table Building

### Unified Table Method

```scala
// Markdown table
.table("markdown", "|Signal|Width|Direction|\n|---|---|---|\n|clk|1|Input|")

// CSV to table conversion
.table("csv", "Signal,Width,Direction\nclk,1,Input\ndata,32,Input")

// Raw content
.table("text", "Custom table format")
```

### Structured Table Helper

```scala
.markdownTable(List("Signal", "Width", "Direction"), 
               List(List("clk", "1", "Input"), List("data", "32", "Input")))
```

## Drawing Building

### Unified Drawing Method

```scala
// Mermaid diagrams
.draw("mermaid", "graph TD\n  A --> B\n  B --> C")

// SVG diagrams
.draw("svg", "<svg><rect x='10' y='10' width='100' height='50'/></svg>")

// ASCII art
.draw("ascii", "┌─────┐\n│ CPU │\n└─────┘")

// PlantUML
.draw("plantuml", "@startuml\nAlice -> Bob: Hello\n@enduml")
```

## Code Building

### Language-specific Code

```scala
.code("scala", "class Counter extends Module { ... }")
.code("verilog", "module counter(...); endmodule")
.code("json", """{"config": "value"}""")
```

## Complete Example

```scala
val processorSpec = CONTRACT("PROCESSOR_CORE").desc("Processor core specification")
  .status("DRAFT")
  .entry("Author", "Hardware Team")
  
  // Hierarchical features
  .listItem("Core Features", 0)
  .listItem("RISC-V ISA", 1)
  .listItem("RV32I Base", 2)
  .listItem("RV32M Extension", 2)
  .listItem("Pipeline", 1)
  .listItem("5-stage pipeline", 2)
  .listItem("Branch prediction", 2)
  
  // Signal table from CSV
  .table("csv", """Signal,Width,Direction,Description
clk,1,Input,Clock signal
rst,1,Input,Reset signal
pc,32,Output,Program counter
instr,32,Input,Instruction""")
  
  // Architecture diagram
  .draw("mermaid", """graph TD
    IF[Instruction Fetch] --> ID[Instruction Decode]
    ID --> EX[Execute]
    EX --> MEM[Memory Access]
    MEM --> WB[Write Back]""")
  
  // Verilog interface
  .code("verilog", """module processor_core(
    input clk, rst,
    output [31:0] pc,
    input [31:0] instr
  );
    // Core implementation
  endmodule""")
  
  .note("This processor implements the RISC-V RV32IM instruction set")
  .build()
```

## Benefits

1. **Unified API**: Single methods for each content type with type specification
2. **Hierarchical Structure**: Natural nested list support
3. **Type Flexibility**: Support for multiple input formats (CSV, markdown, etc.)
4. **Markdown Compatible**: Automatic markdown generation
5. **Extensible**: Easy to add new types (plantuml, etc.)

## Migration

### Old multiple methods:
```scala
.table("|A|B|\n|1|2|")
.drawMermaid("graph TD", "A --> B")
.drawSvg("<svg>...</svg>")
```

### New unified approach:
```scala
.table("markdown", "|A|B|\n|1|2|")
.draw("mermaid", "graph TD\nA --> B")
.draw("svg", "<svg>...</svg>")
```
