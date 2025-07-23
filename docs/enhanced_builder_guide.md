# Enhanced Builder Guide - 2025 Updates

This guide covers the enhanced features and improvements made to the Spec Framework in 2025.

## Overview

The Spec Framework has been significantly improved to provide better usability and flexibility while maintaining backward compatibility.

## Key Improvements

### 1. Enhanced uses() Method

The `uses()` method constraints have been relaxed to allow more natural specification relationships:

#### PARAMETER Specs - Universal Usage

PARAMETER specs can now be referenced by any category, enabling better parameterization:

```scala
object MySpecs {
  // Define parameters that can be used by any spec type
  val BusWidth = PARAMETER("BUS_WIDTH")
    .desc("System bus width in bits")
    .meta("default" -> "32")
    .build()
    
  val ClockFreq = PARAMETER("CLOCK_FREQ")
    .desc("System clock frequency")
    .meta("default" -> "100MHz")
    .build()
    
  // BUNDLE can use PARAMETER
  val SystemBus = BUNDLE("SYSTEM_BUS")
    .desc("Main system bus")
    .uses(BusWidth)
    .build()
    
  // FUNCTION can use PARAMETER  
  val AdderFunc = FUNCTION("ADDER_FUNC")
    .desc("N-bit adder function")
    .uses(BusWidth)
    .build()
    
  // INTERFACE can use PARAMETER
  val MemInterface = INTERFACE("MEM_INTERFACE")
    .desc("Memory interface")
    .uses(BusWidth, ClockFreq)
    .build()
}
```

#### CONTRACT to CONTRACT References

CONTRACT specs can now reference other CONTRACT specs, enabling hierarchical system specifications:

```scala
object SystemSpecs {
  val CPUContract = CONTRACT("CPU_CONTRACT")
    .desc("CPU core specification")
    .build()
    
  val MemoryContract = CONTRACT("MEMORY_CONTRACT")
    .desc("Memory subsystem specification")
    .build()
    
  val SOCContract = CONTRACT("SOC_CONTRACT")
    .desc("System-on-chip specification")
    .uses(CPUContract, MemoryContract)  // Now allowed!
    .build()
}
```

### 2. Improved table() Method

The `table()` method now supports both single and double parameter forms for better usability:

#### Single Parameter (Shorthand)

```scala
val TableSpec = INTERFACE("TABLE_DEMO")
  .desc("Table method demonstration")
  // Defaults to markdown table
  .table("""
| Register | Address | Access |
|----------|---------|--------|
| CTRL     | 0x0000  | R/W    |
| STATUS   | 0x0004  | R      |
| DATA     | 0x0008  | R/W    |
""")
  .build()
```

#### Two Parameter (Explicit Type)

```scala
val DiagramSpec = INTERFACE("DIAGRAM_DEMO")
  .desc("Diagram method demonstration")
  // Explicit table type
  .table("mermaid", """
graph TD
  A[CPU] --> B[Cache]
  B --> C[Memory]
  C --> D[Storage]
""")
  .build()
```

### 3. Comprehensive Relationship Support

The framework now supports complex specification relationships:

```scala
object ComplexSpecs {
  // Parameters
  val DataWidth = PARAMETER("DATA_WIDTH").desc("Data path width").build()
  val AddrWidth = PARAMETER("ADDR_WIDTH").desc("Address width").build()
  
  // Interfaces using parameters
  val BusInterface = INTERFACE("BUS_INTERFACE")
    .desc("System bus interface")
    .uses(DataWidth, AddrWidth)
    .build()
    
  // Bundles using parameters and having interfaces
  val ProcessorBundle = BUNDLE("PROCESSOR_BUNDLE")
    .desc("Processor signal bundle")
    .uses(DataWidth)
    .has(BusInterface)
    .build()
    
  // Functions using parameters
  val ALUFunction = FUNCTION("ALU_FUNCTION")
    .desc("Arithmetic logic unit")
    .uses(DataWidth)
    .build()
    
  // Contracts tying everything together
  val CPUContract = CONTRACT("CPU_CONTRACT")
    .desc("Complete CPU specification")
    .uses(DataWidth, AddrWidth)
    .has(ProcessorBundle, BusInterface)
    .is(ALUFunction)
    .build()
}
```

## Migration Guide

### From Old API to New API

The enhanced framework is fully backward compatible. Existing code will continue to work without changes.

#### Existing Code (Still Works)

```scala
val oldSpec = CONTRACT("OLD_SPEC")
  .desc("Legacy specification")
  .table("markdown", "|A|B|\n|C|D|")
  .build()
```

#### Enhanced Code (New Features)

```scala
val newSpec = CONTRACT("NEW_SPEC")
  .desc("Enhanced specification")
  .table("|A|B|\n|C|D|")  // Shorthand table
  .uses(someParameter)     // Can now use PARAMETER
  .build()
```

## Best Practices

### 1. Use Parameters for Reusability

```scala
// Define common parameters once
val CommonParams = Seq(
  PARAMETER("DATA_WIDTH").desc("Standard data width").build(),
  PARAMETER("ADDR_WIDTH").desc("Standard address width").build()
)

// Reuse in multiple specs
val MemSpec = INTERFACE("MEMORY").uses(CommonParams: _*).build()
val BusSpec = INTERFACE("BUS").uses(CommonParams: _*).build()
```

### 2. Build Hierarchical Contracts

```scala
// Bottom-up approach
val ModuleContract = CONTRACT("MODULE").desc("Individual module").build()
val SubsystemContract = CONTRACT("SUBSYSTEM").uses(ModuleContract).build()
val SystemContract = CONTRACT("SYSTEM").uses(SubsystemContract).build()
```

### 3. Use Shorthand Methods

```scala
// Prefer shorthand for common cases
.table("|Col1|Col2|\n|Val1|Val2|")  // Instead of .table("markdown", ...)
.code("val x = 1")                  // Instead of .code("scala", ...)
```

## Error Handling

The enhanced framework provides better error messages:

```scala
// This will give a clear error message
val invalidSpec = FUNCTION("TEST")
  .uses(someContractSpec)  // Error: FUNCTION cannot use CONTRACT (except PARAMETER)
  .build()
```

## Summary

The 2025 enhancements provide:

- **Greater Flexibility**: PARAMETER specs can be used by any category
- **Hierarchical Support**: CONTRACT specs can reference other CONTRACTs  
- **Better Usability**: Shorthand methods for common operations
- **Backward Compatibility**: All existing code continues to work
- **Clear Error Messages**: Better feedback for invalid operations

These improvements make the Spec Framework more powerful while keeping it easy to use.
