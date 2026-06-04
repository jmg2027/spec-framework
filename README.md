# Spec Framework

This repository contains a proof-of-concept hardware specification management framework.
It is organized as several sbt subprojects:

- **spec-core** – definition of specification data types
- **spec-macros** – macro annotations that collect spec information
- **spec-plugin** – sbt plugin for exporting JSON indexes
- **design** – example project using the framework

The DSL offers categories such as `CONTRACT`, `FUNCTION`, and `INTERFACE`.  The
`BUNDLE` category can be used to document reusable data structures referenced by
interfaces.

## Compiler-verified specs

Beyond documentation and traceability, the framework lets the Scala compiler
verify the spec graph against the implementation:

- **Typed bundles** — `framework.macros.TypedSpec.bundle[T]("…","…")(_.addr, _.txnId)`
  binds a `BUNDLE` to its implementation type `T`. Field name and type are read
  from the selectors, so a renamed/removed field is a **compile error** and an
  undeclared field is a compile error (`bundle`) or warning (`bundleLenient`).
- **Anchoring RTL** — `@LocalSpec(spec)` (Scala-2 annotation) or `localSpec(spec)` /
  `localSpec(spec, decl)` (cross-version method; the only form on Scala 3). See
  [`docs/TAGGING.md`](docs/TAGGING.md) for why both exist and which to use.
- **By-value relations** — `.has(intfFoo)` references other specs by value
  (not by string id), so a wrong reference is a **compile error**; forward and
  cross-file references work.
- **Formal connection** — `framework.macros.Formal.assertProperty(prop){ cond }`
  binds a `PROPERTY`/`COVERAGE` node to a real check and records it; the checker
  reports unenforced properties and emits `properties.sva`.
- **Compliance report** — `framework.spec.SpecCheck` aggregates the artefacts,
  resolves relations, and reports implementation/formal coverage, bundle
  completeness, and dangling references (`--strict` fails the build on warnings).
  It emits machine-readable `SpecIndex.json` / `TagIndex.json` / `properties.sva`
  **and** a human-readable `SPEC.md` (a GitHub-rendered Mermaid graph + tables;
  see [`examples/frontend-demo/SPEC.sample.md`](examples/frontend-demo/SPEC.sample.md)).

- **Spec → design generation** — `framework.spec.SpecGen` reads the spec graph and
  emits a Chisel skeleton (config case class, bundle classes with real widths,
  modules with `localSpec`-anchored ports + sub-modules). The spec leads, the
  design follows.
- **Spec-driven verification** — the same `PROPERTY`/`COVERAGE` spec objects the
  RTL binds are reused as testbench checkers (`framework.spec.Bench`) and as a
  formal harness (`spec_formal.sby`); results fold back so every node shows
  *bound? · passed in sim? · covered? · formal?* in one report. See
  [`docs/SPEC_DRIVEN_VERIFICATION.md`](docs/SPEC_DRIVEN_VERIFICATION.md).

Runnable walk-throughs:
[`examples/frontend-demo`](examples/frontend-demo) (Chisel-free,
`--drift`/`--ref-drift`), [`examples/stream-processor`](examples/stream-processor)
(mid-size **real Chisel** + spec-driven testbench), and
[`examples/stream-processor-s3`](examples/stream-processor-s3) (same graph on
Scala 3).

To build everything offline run:

```bash
./publish.sh
```

After running the script, the files `SpecIndex.json` and `TagIndex.json` will be created in the output directory specified by the `spec.meta.dir` system property in your `build.sbt`. By default, this is `design/target/`, but it may be customized by the user.

See `docs/architecture.md` for an overview, `docs/PLUGIN_USAGE.md` for sbt setup,
`docs/user_guide_213.md` for usage instructions and `docs/developer_guide.md` for
details on hacking the framework. Coding style conventions are documented in
`docs/SCALADOC_STYLE_GUIDE.md`.

For English documentation, refer to
[`docs/plugin_enable_en.md`](docs/plugin_enable_en.md),
[`docs/builder_usage_en.md`](docs/builder_usage_en.md) and
[`docs/localspec_usage_en.md`](docs/localspec_usage_en.md).

For Korean documentation, refer to
[`docs/plugin_enable_ko.md`](docs/plugin_enable_ko.md),
[`docs/builder_usage_ko.md`](docs/builder_usage_ko.md) and
[`docs/localspec_usage_ko.md`](docs/localspec_usage_ko.md)

For detailed information on the new features, see [`docs/enhanced_builder_guide.md`](docs/enhanced_builder_guide.md).

## License

This project is licensed under the MIT License.
Until early 2025 the repository used a proprietary license for asset management between the company and the repository owner. We transitioned to MIT later that year to encourage broader community participation.
See [LICENSE](LICENSE) for details.
