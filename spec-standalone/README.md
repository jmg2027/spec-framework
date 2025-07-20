# Standalone Spec Framework

This is a lightweight, dependency-free version of the Spec framework. Just copy `StandaloneSpec.scala` to your project and use it directly.

## Usage

```scala
// Just copy StandaloneSpec.scala to your project
import framework.specs._

// Define your specs exactly like in the full framework
object MySpecs {
  val memorySpec = Spec.CONTRACT("MEMORY_001")
    .desc("Memory controller interface")
    .status("DRAFT")
    .entry("Author", "Hardware Team")
    .table("markdown", """
| Signal | Width | Direction |
|--------|-------|-----------|
| clk    | 1     | Input     |
| data   | 32    | Input     |
""")
    .draw("mermaid", """
graph TD
  A[Memory Request] --> B[Controller]
  B --> C[Memory]
""")
    .code("scala", """
class MemoryController extends Module {
  val io = IO(new Bundle {
    val req = Input(Bool())
    val data = Input(UInt(32.W))
  })
}
""")
    .build()
}

// Use annotations (they do nothing but allow compilation)
@LocalSpec(memorySpec)
class MyModule {
  // Your hardware code here
}
```

## Features

- **Zero Dependencies**: No need to add libraries to your build
- **Same API**: Identical syntax to the full framework
- **Compilation Support**: Code compiles without errors
- **Easy Migration**: Can easily switch to full framework later

## What It Does

In standalone mode:
- All builder methods return the builder (fluent API works)
- `build()` just prints the spec name (or does nothing)
- `@LocalSpec` is a dummy annotation
- `spec { ... }` macro is a passthrough function

## Migration to Full Framework

When you're ready to use the full framework:

1. Replace the standalone file with proper dependencies:
```scala
libraryDependencies += "your.company" %% "spec-core" % "0.1.0-SNAPSHOT"
```

2. Change the import:
```scala
// From:
import framework.specs._

// To:
import framework.spec.Spec._
import framework.macros.LocalSpec
import framework.macros.SpecEmit.spec
```

3. Your spec definitions remain exactly the same!

## Benefits

- **Prototyping**: Start writing specs immediately
- **No Build Changes**: No need to modify build.sbt
- **Team Adoption**: Share specs without framework setup
- **Gradual Migration**: Adopt the full framework when ready
