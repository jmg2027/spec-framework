# Spec Builder Usage (English)

This guide explains how to write hardware specifications with the `framework.spec.Spec` DSL. It matches the current `Spec.scala` implementation.

## 1. Basic structure

Choose a category, call `desc` for the description, chain any options, and finish with `build()`.

```scala
import framework.macros.SpecEmit.spec
import framework.spec.Spec._

val mySpec = spec {
  CONTRACT("EXAMPLE_ID").desc("Example spec")
    .status("DRAFT")
    .entry("Author", "HW Team")
    .build()
}
```

Within a `spec { ... }` block, calling `build()` generates a meta file at compile time and registers the spec at runtime.

## 2. Categories

The DSL provides several category constructors. Pick one that matches the type of specification you are writing before calling `desc`.

| Category | Usage | Description |
|---------|-------|-------------|
| `CONTRACT` | `CONTRACT(id)` | Top-level contract or requirement. |
| `FUNCTION` | `FUNCTION(id)` | Describes a function or algorithm. |
| `PROPERTY` | `PROPERTY(id)` | Assertion or property specification. |
| `COVERAGE` | `COVERAGE(id)` | Coverage requirement specification. |
| `INTERFACE` | `INTERFACE(id)` | Hardware interface description. |
| `PARAMETER` | `PARAMETER(id)` | Parameter or configuration item. |
| `CAPABILITY` | `CAPABILITY(id)` | Supported capability definition. |
| `BUNDLE` | `BUNDLE(id)` | Reusable data structure referenced by interfaces. |
| `RAW` | `RAW(id, prefix)` | Custom category with the given prefix. |

Example usage:

```scala
val contCoreReq  = spec {
  CONTRACT("CORE_REQ")
  .desc("Core requirement")
   ...
  .entry("Function", "RISCV ISA compliant configurable core")
   ...
  .build()
}

val funcAddFn  = spec {
  FUNCTION("ADD_FN")
  .is(contAlu)
   ...
  .desc("Addition function")
   ...
  .build() 
}

val bndAwChannel    = spec { 
  BUNDLE("AW_CHANNEL")
  .desc("Write Request")
  .has(paramAxiBus)
   ...
  .entry("id", "Transaction identifier for the write channels")
  .entry("addr", "Transaction address")
   ...
  .build()
}

val intfAxiBus = spec {
  INTERFACE("AXI_BUS")
    .desc("Bus interface")
     ...
    .has(bndAwChannel)
     ...
    .build()
}

val rawAsyncClockSdc = spec {
  RAW("RAW_ASYNC_CLOCK", "SDC")
  .desc("async clock groups in this module/domain")
   ...
  .entry("coreClk", "periClk")
   ...
  .code("sdc", "set_async_group [ ... ]")
  .build()
```

## 3. Stage2 methods

After calling `desc` you can use these methods - all optional except for build():

| Method | Description |
|-------|-------------|
| `status(String)` | Sets the status value of the spec. |
| `is(spec* )` | Reference another spec ID or object. |
| `has(spec* )` | Declare child spec IDs or objects. |
| `uses(spec* )` | Express dependencies between CONTRACT specs. |
| `entry(key, value)` | Add a key-value item. If `value` is omitted it becomes a hierarchical list item. |
| `table(tableType, content)` | Add a table string such as Markdown or CSV. |
| `markdownTable(headers, rows)` | Build a Markdown table from lists. |
| `draw(drawType, content)` | Insert a diagram such as mermaid, svg or ascii. |
| `code(language, content)` | Insert a code block. Default language is `text`. |
| `note(text)` | Add a note or additional comment. |
| `build()` | Finalize the spec and register it. |

For hierarchical lists include indentation in the first argument to `entry`.

```scala
val spec = FUNCTION("PIPELINE").desc("Pipeline behavior")
  .entry("- stages")
  .entry("  - IF")
  .entry("  - ID")
  .entry("  - EX")
  .build()
```

## 4. Example

```scala
val example = spec {
  INTERFACE("BUS").desc("Bus interface")
    .is("DMA_CONTROLLER")
    .entry("addr", "Address input")
    .table("csv", "Signal,Width\naddr,32")
    .draw("mermaid", "graph TD; A-->B")
    .code("verilog", "module bus(...);")
    .note("Additional description")
    .build()
}
```

The spec is registered in `SpecRegistry` and saved to JSON when you run `exportSpecIndex`.
