# Spec Framework Architecture

This document summarizes how specifications and tags are collected.

## Workflow
1. **User code** defines a `HardwareSpecification` and tags modules or values with `@LocalSpec`.
2. **Macro expansion** evaluates arguments and injects calls to `SpecRegistry.addSpec` and `addTag`.
3. **Runtime** execution of the generated code populates the in-memory registry when the tagged modules are instantiated.
4. The **sbt plugin** exports the registry to `SpecIndex.json` and `TagIndex.json`.

## Components
- **spec-core** – data types for specs and tags
- **spec-macros** – implements `@LocalSpec`
- **SpecRegistry** – runtime buffers for collected data
- **export task** – writes JSON for external tools

This architecture bridges specification and implementation so that documentation and linting tools can operate on accurate, traceable data.
