# Spec Framework Architecture

This document summarizes how specifications and tags are collected.

## Workflow
1. **User code** defines a `HardwareSpecification` (via `spec { … }` / `bundle[T]`)
   and tags modules or values with `@LocalSpec` / `assertProperty`.
2. **At compile time** the macros emit one artefact per node into the directory
   named by `-Dspec.meta.dir`: `spec { … }` and `bundle[T]` write `.spec` files,
   `@LocalSpec` and `assertProperty`/`coverProperty` write `.tag` files
   (`MetaFile.writeSpec` / `writeTag`). No program run is required.
3. The **sbt plugin** (or `SpecCheck`) cleans + aggregates those `.spec`/`.tag`
   files after compilation and writes `SpecIndex.json` and `TagIndex.json`.
4. `SpecRegistry` is an optional *runtime* mirror of the specs (for tests /
   inspection); it is not part of the export path.

## Components
- **spec-core** – data types (`HardwareSpecification`, `Tag`), the typed
  `bundleSpec`, and the aggregator/checker `SpecCheck`
- **spec-macros** – `@LocalSpec`, the `spec { … }` emitter, the typed `bundle[T]`
  macro, and `assertProperty`/`coverProperty`
- **MetaFile** – compile-time `.spec`/`.tag` emission
- **export task / SpecCheck** – aggregate the artefacts into JSON for external tools

This architecture bridges specification and implementation so that documentation and linting tools can operate on accurate, traceable data.
