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

### 1a. `localSpec` loses per-declaration granularity
On Scala 3 (and when used on Scala 2) `localSpec(intf)` is a *statement*, so its
anchor's `scalaDeclarationPath` is the enclosing **module**, not the specific
port/val it documents. Every interface of a module ends up anchored to the same
class line. `@LocalSpec` (the annotation) does not have this problem, but it is
Scala-2-only and experimental/broken on Scala 3.
**Suggestion:** a value-returning form, `val in = localSpec(intf)(IO(...))`, that
anchors to the wrapped declaration and threads the value through.

### 1b. Parameters are a second source of truth
`SPConfig(dataBytes = 4, …)` (the real knobs) and `ParamSpecs` (the `PARAMETER`
specs, `default = "4"`) are **separate** and can drift — exactly the failure mode
the typed bundles eliminate for fields. There is no `paramSpec[SPConfig](_.dataBytes)`
equivalent that reads the default/type from the case class.
**Suggestion:** a typed parameter binding mirroring `bundle[T]`.

### 1c. Bundle field types drop their width/parameters
`bundle[StreamBeat](_.data, …)` records `data: UInt` but not `UInt(dataWidth.W)`.
Widths are config-derived and never make it into the spec, so the spec can't be
checked against actual wire widths.
**Suggestion:** capture the Chisel `Data`'s width at elaboration and fold it into
the bundle spec.

## 2. Authoring ergonomics

- **Spec/design ratio ≈ 1.5:1.** The 5-module design is ~150 lines; its specs are
  ~250. Most of it is mechanical (`INTERFACE("…").desc("…").has(bnd).build()`).
  A scaffolder (generate spec stubs from a tagged design, or vice versa) would
  remove most of the boilerplate and is the single biggest ROI lever.
- **Relation imports pile up.** By-value relations are great for safety, but
  `TopSpecs` must `import` all five module spec objects to reference their
  contracts. Fine at this size; tedious at 30 modules.
- **`.uses` enforcement is now a no-op.** Since by-value relations are rewritten
  to `@fqn` strings before evaluation, the old "only PARAMETER/CONTRACT may be
  `uses`d" check never fires. Either restore it at the checker level or drop it
  from the docs.

## 3. Workflow / tooling

- **Scala 3 needs a run step that touches every spec object.** Specs emit at run
  time, so `Main` must reference each spec object or it silently emits nothing —
  easy to forget, and the node just goes "missing". An sbt task that discovers
  and forces spec objects (or a compile-time emission path on Scala 3) would
  remove the footgun.
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
