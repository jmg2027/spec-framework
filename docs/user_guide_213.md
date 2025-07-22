# Spec Framework User Guide (Scala 2.13)

This guide explains how to define hardware specifications, tag your RTL code and export the JSON index files.  All examples assume Scala 2.13 and sbt.

## 1. Writing Specifications

A spec is a `HardwareSpecification` value constructed with the DSL provided by the `Spec` object. Each spec requires an ID and a short description.

```scala
import framework.spec._
import Spec._

object MySpecs {
  val QueueSpec = FUNCTION(
    id = "QUEUE_FUNCTION_001",
    desc = "FIFO queue must preserve ordering"
  ).capability(Capability("Queueing"))
   .status("DRAFT")
   .meta("author" -> "Alice")
   .entry("Purpose", "Ensures FIFO semantics")
   .build()
}
```

Call `.build()` at the end so the spec is registered in `SpecRegistry` and written to a meta file.

### BUNDLE Specs

Use the `BUNDLE` category to describe reusable data structures referenced by interfaces.

```scala
val FetchReq = spec {
  BUNDLE("BND_FETCH_REQ").desc("Fetch request bundle")
    .entry("pc", "Program counter [type: UInt, width: 32]")
    .entry("valid", "Request valid flag [type: Bool]")
    .build()
}

val BusIntf = spec {
  INTERFACE("INTF_BUS").desc("Example interface")
    .entry("req", "Request channel").has(FetchReq)
    .build()
}
```

## 2. Tagging Code

Use the `@LocalSpec("SPEC_ID")` annotation to mark modules, vals or defs that implement or verify a spec.

```scala
import chisel3._
import framework.macros.LocalSpec
import MySpecs._

@LocalSpec("QUEUE_FUNCTION_001")
class QueueModule extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(UInt(32.W)))
    val deq = Decoupled(UInt(32.W))
  })

  // implementation here
}
```

Annotations may also be placed on `val` or `def` declarations. Use a dummy `val` if you need to tag a specific block of logic inside a method.

## 3. Building and Running

1. Ensure the spec definitions are compiled first so the `.spec` files are generated.
2. Build and run your design project (`sbt run` or `sbt test`). The registry is populated when the annotated code executes.
3. Invoke the sbt task `exportSpecIndex` to produce `SpecIndex.json` and `TagIndex.json` under `design/target`.

The `publish.sh` script in the repository publishes all modules and runs these steps for you.

## 4. JSON Files

- **SpecIndex.json** lists every spec along with the tags that reference it. Use this file as the single source of truth for documentation tools.
- **TagIndex.json** summarizes which specs are associated with each module.

Both files are validated by the test instructions in `AGENTS.md`.

## 5. Tips

- Each hardware module should have at least one contract spec tagged with `@LocalSpec`.
- Keep spec IDs unique and descriptive.
- Place your spec objects under `design/src/main/scala/.../specs` so they are easy to locate.
- When debugging, print the contents of `SpecRegistry.allSpecs` and `SpecRegistry.allTags` after running.

