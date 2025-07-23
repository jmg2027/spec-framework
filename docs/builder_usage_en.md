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

## 2. Stage2 methods

After calling `desc` you can use these methods:

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
| `build(scalaDeclarationPath)` | Finalize the spec and register it. |

For hierarchical lists include indentation in the first argument to `entry`.

```scala
val spec = FUNCTION("PIPELINE").desc("Pipeline behavior")
  .entry("- stages")
  .entry("  - IF")
  .entry("  - ID")
  .entry("  - EX")
  .build()
```

## 3. Example

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
