# Spec Framework Architecture (after `@LocalSpec(HardwareSpecification)` enhancement)

---

## Overview

The spec-framework is a macro-based infrastructure for associating formal HardwareSpecifications with concrete RTL source locations. It enables compile-time spec definition, runtime tag registration, and structured JSON export for automated tooling.

The recent enhancement to support `@LocalSpec(specVal)` (i.e., referencing a `val` that holds a built `HardwareSpecification`) changes the architecture by allowing non-literal arguments to annotations. This requires runtime evaluation of the `val` inside the macro JVM to extract the spec ID.

---

## Layered System Architecture

### 1. User Code

```scala
val MySpec = emitSpec { ... }.build()
@LocalSpec(MySpec)
class MyModule extends Module
```

* Users define `HardwareSpecification` via the staged DSL.
* Use `@LocalSpec(MySpec)` to tag a module with the defined spec.

### 2. Macro Expansion (Compile-Time)

* The `@LocalSpec(...)` macro is expanded.
* If the argument is a `String`, it is used directly as `specId`.
* If the argument is a `HardwareSpecification`-typed expression:

  * The macro evaluates the expression using `c.eval(...)`.
  * It extracts the `.id` field from the resulting object.
* A `Tag` object is constructed using:

  * `specId`
  * source file path
  * line number
  * class/module name

### 3. Code Generation

* The macro injects runtime code:

```scala
SpecRegistry.addSpec(MySpec)
SpecRegistry.addTag(Tag(...))
```

* This code will run only when the annotated class or its enclosing module is instantiated.

### 4. Runtime Execution

* At runtime (during `sbt run`, `sbt test`, etc.), the generated `addSpec` and `addTag` calls are executed.
* This populates the global `SpecRegistry` in-memory buffers.

### 5. SpecRegistry Buffer

* Two buffers exist:

  * `ListBuffer[HardwareSpecification]`
  * `ListBuffer[Tag]`
* These accumulate all definitions and tags encountered during runtime.

### 6. JSON Export

* A plugin or runtime tool extracts the contents of `SpecRegistry`.
* Two files are written:

  * `spec-registry.json` (spec metadata)
  * `spec-usage.json` (tag mappings)
* These files become the canonical data sources for documentation, coverage, linting, and dashboards.

---

## Supporting Components

### Macro JVM Runtime (compile-time)

* Executes `c.eval(...)` safely within the macro expansion context.
* Requires that `val` definitions are pure and safe to evaluate (e.g., emitSpec DSL only).

### SpecPlugin / Export Task

* Can be integrated as an `sbt` task:

```scala
sbt specExport
```

* Reads all registered specs and tags and outputs JSON.

### Spec Consumers

* `spec2md`: generates Markdown documentation
* `spec-dashboard`: HTML dashboard for visualization
* `spec-lint`: verifies spec coverage and tagging
* `spec-coverage`: correlates tests to specifications

---

## Architectural Advantages

| Feature                           | Benefit                                                   |
| --------------------------------- | --------------------------------------------------------- |
| Literal and object specId support | Flexible, expressive annotation interface                 |
| Macro-assisted static linking     | Compile-time safety with runtime traceability             |
| Structured spec registry          | Enables toolchain automation and spec compliance tracking |
| JSON-based export                 | Allows integration with downstream tools and dashboards   |
| Backward compatibility            | Old string-based `@LocalSpec("ID")` remains functional    |

---

## Summary

This enhanced architecture allows the spec framework to move beyond string-literal-based tagging, enabling statically typed, semantically rich annotations. By combining macro-time extraction and runtime registry accumulation, it bridges the gap between specification and implementation in a traceable, automatable manner.
