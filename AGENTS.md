

# AI Agent Integration Guide

## 1. Build & Test

- All code changes must be validated by running `./publish.sh` at the repository root.
- Do not run sbt commands directly for testing; always use `./publish.sh`.


## 2. Artifacts & Validation

After `./publish.sh`, the following JSON files must be created and contain valid content:
  - `SpecIndex.json`
  - `TagIndex.json`

The output directory for these files is user-configurable via the `spec.meta.dir` system property in your `build.sbt`. By default, they are written to `design/target/`, but you may override this location as needed. Always check your `build.sbt` for the current output path.

Each file must contain at least one entry.
The first object in each file must include:
  - `SpecIndex.json`: `id`, `category`
  - `TagIndex.json`: `scalaDeclarationPath`, `srcFile`

Artifacts must match the golden files after sorting (adjust the paths if you change the output directory):
  - `design/golden/SpecIndex.golden.json` vs `<output-dir>/SpecIndex.sorted.json`
  - `design/golden/TagIndex.golden.json` vs `<output-dir>/TagIndex.sorted.json`

## 3. sbt & Offline Environment

- All sbt commands must be run from `/workspace/spec-framework`.
- Always use these JVM options:
  - `-Dsbt.ivy.home=/workspace/.sbt-cache/.ivy2`
  - `-Dsbt.global.base=/workspace/.sbt-cache/.sbt`
- The environment is offline; dependencies must be pre-cached.
- Any change to `build.sbt` or `plugins.sbt` requires a full environment rebuild.

## 4. Maintenance & References

- To regenerate the cache, run `sbt update` locally, archive `.ivy2` and `.sbt`, and upload to `/workspace/.sbt-cache/`.
- For architecture, usage, and development details, see the `docs/` directory.
