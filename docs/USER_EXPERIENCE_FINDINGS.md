# User-experience findings

Notes from actually building a mid-size design (the `stream-processor` example:
5 modules + top, configurable, Chisel 7) with the framework. Grounded in real
friction, not speculation. Prioritised.

## 0. Fixed while building (so the example would be promotion-grade)

These were real blockers/annoyances hit during the build and fixed in-place:

- **Unreadable bound conditions.** Chisel desugars `a && b` into
  `a.do_$amp$amp(b)(using SourceInfo)`; the report/SVA/`SPEC.md` showed that
  monster. Now the macro records the **verbatim source** (`!(in.fire &&
  !q.io.enq.fire)`).
- **`assertProperty` only accepted `Boolean`.** Chisel conditions are
  `chisel3.Bool`. Made the macros generic in the condition type.
- **Typed-bundle completeness counted inherited fields.** `bundle[T]` over a
  chisel3 `Bundle` flagged base-class members. Now checks the bundle's *own*
  declared fields.
- **Dependency friction:** `upickle` version clash with Chisel (bumped to 3.x),
  and Chisel's SIP-51 forces `scalaVersion ≥ 2.13.16` (a cryptic eviction error).

## 1. High-impact gaps

### 1a. `localSpec` loses per-declaration granularity — ✅ FIXED
`localSpec(intf)` (statement) anchored to the enclosing module. Added a
value-returning form `val in = localSpec(intf, IO(...))` that anchors to the
wrapped declaration and threads the value through (both Scala 2 and 3). Interfaces
now anchor per-port — `INTF_INGRESS_IN → IngressBuffer.in`, not the class.

### 1b. Parameters are a second source of truth — ✅ FIXED (build-time pure)
Two forms exist; both read the field name/type from the selector, so a rename is a
compile error either way:

- **`param[T](id, desc, default)(_.field)` — emitted at COMPILE time (canonical).**
  The `.spec` is written during `sbt compile` (like `bundle` / `spec { … }`), so
  the spec graph stays a *pure build artefact* — no Chisel run/elaboration is
  needed to materialise it. This matches the real target: a production Chisel/ASIC
  build whose CI gate runs on `compile`. The trade-off is that the default is
  restated as a compile-time literal; the selector still binds name/type to the
  config field.
- **`paramSpec[T](id, desc, cfg)(_.field)` — emitted at RUN time (opt-in).** The
  `default` is read from a live `cfg` instance, so the default value is also
  single-sourced — at the cost of needing a run to emit. Use it only when
  single-sourcing the default matters more than build-time purity.

The example's `ParamSpecs` uses `param` (build-time). Earlier this finding shipped
only `paramSpec`, which regressed build-time purity (params then needed a run);
`param` restores the compile-time path while keeping rename-safety.

### 1c. Bundle field types drop their width/parameters — ✅ FIXED
The static macro can only see `data: UInt`, not the config-derived width. Added a
`WidthProbe` (HDL-side) that constructs each bundle and reads `chisel3 .getWidth`,
writing `BundleWidths.json`; `SpecCheck.applyWidths` merges it so the spec shows
`data: UInt(32)`, `last: Bool(1)`, etc. (kept out of the Chisel-agnostic core —
the probe is a thin HDL tool, the merge is a side-file).

## 2. Authoring ergonomics

- **Spec/design ratio ≈ 1.5:1.** The 5-module design is ~150 lines; its specs are
  ~250. ✅ Addressed in the *correct* direction — **spec → design**: `SpecGen`
  reads the spec graph and emits a Chisel skeleton (config case class, bundle
  classes *with real widths*, modules with `localSpec`-anchored ports + sub-module
  instances + spec-object imports). The spec is the source of truth, so the design
  skeleton is generated from it (not specs extracted from an existing design).
- **Relation imports pile up.** By-value relations are great for safety, but
  `TopSpecs` must `import` all five module spec objects to reference their
  contracts. Fine at this size; tedious at 30 modules.
- **`.uses` enforcement is now a no-op.** Since by-value relations are rewritten
  to `@fqn` strings before evaluation, the old "only PARAMETER/CONTRACT may be
  `uses`d" check never fires. Either restore it at the checker level or drop it
  from the docs.

## 3. Workflow / tooling

- **Scala 3 needs a run step that touches every spec object.** On Scala 3 most
  specs emit at run time, so `Main` must reference each spec object or it silently
  emits nothing — easy to forget, and the node just goes "missing". (Partially
  eased: `bundle`, `spec { … }` on Scala 2 and now `param` on *both* versions emit
  at compile time, so those nodes never depend on a run.) An sbt task that
  discovers and forces the remaining run-time spec objects would close the gap.
- **No live feedback.** The loop is compile → run → check. "This contract isn't
  anchored" only shows after `SpecCheck`. An incremental/editor surfacing of
  coverage would tighten it.
- **`properties.sva` isn't drop-in SV.** The conditions reference Scala/Chisel
  identifiers (`in.fire`, `q.io.enq.fire`), not SystemVerilog signal names, so the
  file is scaffolding, not a directly bindable SVA module. A name-mapping (from
  the elaborated FIRRTL) would make it real.
- **No spec-graph diff.** For a real project, `git`-diffing `SpecIndex.json` is
  noisy; a semantic diff ("INTF_X added, FUNC_Y lost its anchor") would help in
  review.

## 4. What already works well (keep)

- By-value relations catching typos at compile time, and typed bundles catching
  field drift, are the standout wins — they make the spec *load-bearing*.
- One shared spec graph across Scala 2 (Chisel) and Scala 3 (shim), producing an
  identical report, is a strong portability story.
- `SPEC.md` (Mermaid + coverage + anchors) is genuinely browsable with zero extra
  tooling — the right default human artefact.
