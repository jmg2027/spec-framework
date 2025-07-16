# SpecPlugin Quick Start

This plugin simplifies generation of spec metadata. A typical single-module layout is:

```
design/
  build.sbt
  src/main/scala/your_project/
    specs/   // HardwareSpecification definitions
    design/  // RTL code
```

## Minimal build.sbt
```scala
lazy val design = (project in file("."))
  .enablePlugins(SpecPlugin)
  .settings(name := "design")
```
No extra options are required. Cross-file or cross-package references work with a regular import.

## Using @LocalSpec
```scala
@LocalSpec(QueueSpec)
class MyModule extends Module { ... }
```
`QueueSpec` is a `val` defined in your specs package. The plugin handles cross-module setups via `dependsOn`.

## Build and Run
- `sbt compile` generates `.spec` and `.tag` files
- `sbt run` or `sbt test` populates the registry

Ensure spec modules build first so `.spec` files exist before they are referenced.

### Exporting the Index
Run the following task from the `design` project:

```
sbt exportSpecIndex
```

`SpecIndex.json` and `TagIndex.json` will appear under `design/target`. These files are the primary input to reporting tools.
