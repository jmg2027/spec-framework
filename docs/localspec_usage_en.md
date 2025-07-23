# LocalSpec Macro Usage (English)

The `@LocalSpec` macro marks modules, values or methods that implement a specification. During compilation tag information is written to meta files and later included in the JSON index when `exportSpecIndex` is run.

## 1. Preparation

- Enable `SpecPlugin` and add the `-Ymacro-annotations` option. See [plugin_enable_en.md](plugin_enable_en.md) for details.
- Spec definitions are usually written inside `spec { ... }` blocks and stored as `val`s.

```scala
import framework.macros.SpecEmit.spec
import framework.spec.Spec._

object MySpecs {
  val QueueSpec = spec {
    CONTRACT("QUEUE_CONTRACT").desc("Queue module spec")
      .build()
  }
}
```

## 2. Tagging modules

Pass a spec object or ID string to the annotation for modules, values or methods.

```scala
import chisel3._
import framework.macros.LocalSpec
import MySpecs._

@LocalSpec(QueueSpec)
class QueueModule extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(UInt(32.W)))
    val deq = Decoupled(UInt(32.W))
  })
  // implementation
}
```

If you cannot annotate an expression directly, declare a dummy `val`:

```scala
@LocalSpec(QueueSpec)
val tagForWhen = ()
when(io.enq.valid) {
  // ...
}
```

## 3. Running and results

When the annotated code runs, `SpecRegistry` records the tag information. Then execute the following in sbt to generate the JSON files:

```bash
sbt exportSpecIndex
```

`SpecIndex.json` and `TagIndex.json` will be generated under `design/target/` (or your configured path) with the source location of each tag.
