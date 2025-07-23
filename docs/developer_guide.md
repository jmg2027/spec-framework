# Developer Guide

This document explains the internal architecture and build setup for the Spec Framework. Use it when modifying the framework or extending it with new features.

## Project Layout

The repository is an sbt multi-project build:

- **spec-core** – data types and runtime registry
- **spec-macros** – `@LocalSpec` macro implementation
- **spec-plugin** – sbt tasks that export JSON reports
- **design** – example project using the framework

Each subproject has its own `build.sbt`. Cross compilation is used where necessary.

## Core Modules

### spec-core
`spec-core` defines the case classes `HardwareSpecification` and `Tag` along with the `SpecRegistry` object.  All spec definitions call `SpecRegistry.addSpec` and tags are recorded via `SpecRegistry.addTag`.  The JSON serializers use uPickle.

### spec-macros
The `@LocalSpec` annotation registers a tag at compile time. The macro extracts the spec ID string, the source file position and the fully qualified Scala declaration path.  Additional instance path information can be filled in by a FIRRTL transform in the future.

### spec-plugin
This sbt plugin defines the task `exportSpecIndex`. It writes `SpecIndex.json` and `TagIndex.json` based on the data stored in `SpecRegistry`.  Future tasks like `specLint` and `reportGen` can perform linting and documentation generation.

## Build Workflow

1. Run `publishLocal` on `spec-core` and `spec-macros` so that the design project can depend on them as libraries.
2. In the `design` project, enable `SpecPlugin` and define your own specs and hardware modules.
3. Execute `exportSpecIndex` after your tests or simulation runs to produce the JSON index files.

The included `publish.sh` script automates publishing all subprojects in the correct order.

## Troubleshooting Tips

- Make sure `Compile / scalacOptions += "-Ymacro-annotations"` is enabled for projects that use the macro.
- Confirm that `MetaFile.setBaseDir` is called from `build.sbt` so spec metadata is written to the desired directory.
- If `SpecIndex.json` is empty, check that every spec builder ends with `.build()` and that tagged modules are actually instantiated during the run phase.
- Cleaning the sbt cache (`rm -rf ~/.ivy2/cache ~/.sbt/boot`) can resolve stale dependency issues.

## Future Extensions

- **FIRRTL Transform**: populate `fullyQualifiedModuleName` and `hardwareInstancePath` for each tag.
- **Lint Rules**: analyse `SpecIndex.json` and warn if modules lack a contract spec or if specs are unused.
- **Report Generation**: combine the JSON files with verification logs to produce HTML or PDF reports.


## Directory Structure

```
project-root/
  build.sbt           <- root build file
  project/            <- sbt configuration for the build itself
  spec-core/          <- core data model
  spec-macros/        <- macro annotation
  spec-plugin/        <- sbt plugin
  design/             <- example usage project
```

Each subproject publishes a local artifact that the others depend on via `libraryDependencies`. Avoid using `dependsOn` across repository boundaries.

### Cross Version Sources

Source files under `src/main/scala-2.13/` or `src/main/scala-2.12/` are compiled only for that Scala version. Shared code goes under `src/main/scala/`.

### Publishing Order

`spec-core` -> `spec-macros` -> `spec-plugin` -> `design`

The `publish.sh` script performs this sequence and runs `exportSpecIndex` in the design project to generate the JSON output.

## Adding a New Spec

1. Create a spec object with the DSL in `design/src/main/scala/.../specs` and call `.build()`.
2. Tag modules, vals or defs with `@LocalSpec("SPEC_ID")`.
3. Run the design project to execute the tagged code so the registry is populated.
4. Call `exportSpecIndex` to produce the JSON files.

## Style Guide

Follow the conventions in `docs/SCALADOC_STYLE_GUIDE.md` when writing code and documentation. Consistent headers and scaladoc comments make the generated reports easier to read.

## 2025 Framework Enhancements

### Key Changes Made

1. **Enhanced uses() Method Logic** (spec-core/Spec.scala)
   - Modified `idsFrom()` method to allow PARAMETER specs in all categories
   - Added support for CONTRACT-to-CONTRACT references
   - Improved error messages for invalid relationships

2. **Table Method Overloading** (spec-core/Spec.scala)
   - Added single parameter `table(content: String)` method
   - Maintains backward compatibility with `table(type: String, content: String)`
   - Default table type is "markdown"

3. **Comprehensive Testing** (design/SpecFrameworkTests.scala)
   - Added test cases for all new relationship types
   - Verified PARAMETER universal usage
   - Tested CONTRACT hierarchical relationships

### Implementation Details

#### Enhanced Relationship Logic

```scala
private def idsFrom(args: Seq[Any], enforceContract: Boolean = false): Set[String] = {
  args.map {
    case s: HardwareSpecification =>
      if (enforceContract) {
        // PARAMETER can be used by any category
        if (s.category == SpecCategory.PARAMETER) {
          s.id
        }
        // CONTRACT can use other CONTRACTs
        else if (core.cat == SpecCategory.CONTRACT && s.category == SpecCategory.CONTRACT) {
          s.id
        }
        // Other combinations are restricted
        else {
          throw new IllegalArgumentException(...)
        }
      } else {
        s.id  // No enforcement for is() and has()
      }
    // ...
  }.toSet
}
```

#### Table Method Enhancement

```scala
// Primary method with explicit type
def table(tableType: String, content: String): Stage2 = {
  copy(core.copy(tables = core.tables :+ Table(tableType, content)))
}

// Convenience method with default type
def table(content: String): Stage2 = table("markdown", content)
```

### Testing and Validation

All changes have been thoroughly tested with:

- **Compilation tests**: Ensuring all new syntax compiles correctly
- **Runtime tests**: Verifying spec registration and JSON output
- **Relationship tests**: Confirming allowed/disallowed spec relationships
- **Backward compatibility**: Ensuring existing code continues to work

### Future Development Guidelines

When extending the framework:

1. **Maintain Backward Compatibility**: Always provide migration paths
2. **Test Thoroughly**: Add comprehensive test cases for new features
3. **Document Changes**: Update all relevant documentation
4. **Follow Patterns**: Use established code patterns and naming conventions
5. **Consider Performance**: Ensure changes don't impact build times significantly

