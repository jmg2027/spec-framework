# frontend-demo — typed bundles, formal connection, by-value relations

A small, self-contained example that exercises two enhancements to the spec
framework on a slice of a RISC-V-style instruction **fetch unit**:

1. **Typed bundles (타입화)** — a `BUNDLE` spec is bound to the *implementation
   type*, so spec↔RTL **field drift becomes a compile error** instead of a
   silent JSON diff.
2. **Formal connection (formal 연결)** — a `PROPERTY`/`COVERAGE` spec is bound to
   a real boolean check, so the index records *what* is enforced and the checker
   reports *which declared properties have no check at all*.
3. **By-value relations** — `.has(intfEpmReqOut)` references other specs *by
   value*, so a wrong reference is a **compile error** (the compiler guarantees
   the referenced spec exists), and forward references across the file work.

It is deliberately **Chisel-free**: a ~15-line shim (`minichisel`) stands in for
`chisel3` so the demo builds in seconds with no heavyweight dependencies. The
spec/design code is identical against real Chisel — see *Mapping to Chisel*
below.

## Run it

```bash
./run-demo.sh             # build, emit indices, print the compliance report
./run-demo.sh --drift     # rename an implementation field → spec fails to compile
./run-demo.sh --ref-drift # typo a relation's spec reference → spec fails to compile
```

(Needs a JDK and `sbt`; override the launcher with `SBT=/path/to/sbt`.)

## Layout

| module        | what it is                                              |
|---------------|---------------------------------------------------------|
| `types/`      | `minichisel` shim + bundle types + params (no specs)    |
| `specs/`      | the spec graph — typed `bundle[T]`, FUNCTIONs, PROPERTYs … |
| `design/`     | the RTL, anchored to specs via `@LocalSpec` / `assertProperty` |

Compiled in dependency order (`types → specs → design`) so the typed-bundle
reflection and the `@LocalSpec` id-resolution always see already-compiled
upstream artefacts.

## Feature 1 — typed bundles

```scala
// specs/…/FetchSpecs.scala     bundle[T](id, desc, usesParamIds*)(fieldSelectors*)
val bndFetchReq =
  bundle[FetchRequest]("BND_FETCH_REQUEST", "EPM fetch request",
    "PARAM_PC_WIDTH", "PARAM_TXNID_WIDTH")(_.addr, _.txnId)
```

`bundle[T]` is a **macro**. It reads each field's NAME and TYPE straight from the
selector tree — `_.addr` ⇒ name `"addr"`, type `UInt` — so there is no string to
drift from the selector, the spec can't misreport the type, and the `.spec` is
emitted at **compile time** (no runtime step). The emitted entry:

```json
"lists": [ ["addr", "UInt"], ["txnId", "UInt"] ]   // types read from the RTL
```

Rename `addr` in the implementation and the **spec** stops compiling
(`./run-demo.sh --drift`):

```
FetchSpecs.scala:46:48: value addr is not a member of frontend.demo.types.FetchRequest
did you mean address?
```

Completeness is checked against the type's members at compile time and is
**strict by default**: `bundle` makes an undeclared field a **compile error**
(the spec must describe the whole bundle). `BND_INSTR_SLOT` opts out with
`bundleLenient` to intentionally leave `valid` undeclared — that downgrades it to
a **compile warning** plus a checker note.

## Feature 2 — formal connection

```scala
// design/…/FetchUnit.scala
val noOverflow = assertProperty(propNoOverflow) { (reqCount - respCount) <= maxOutstanding }
assert(noOverflow, "request table overflow")
```

`assertProperty` records `propertyId ⇐ conditionText ⇐ sourceLocation` into the
index (the condition is rendered as clean infix, e.g.
`(reqCount - respCount) <= maxOutstanding`) and returns the condition so it can
drive a real `assert`/`cover`. A declared property with *no* binding
(`PROP_EPOCH_FLUSH_CLEAN`) is reported as **unenforced**.

The checker then exports those bindings to **`properties.sva`** — the concrete
formal artifact the bindings feed:

```systemverilog
ap_PROP_NO_REQTABLE_OVERFLOW: assert property (@(posedge clk) disable iff (reset)
    ((reqCount - respCount) <= maxOutstanding));
// TODO PROP_EPOCH_FLUSH_CLEAN — An epoch flush clears every outstanding entry
```

(In the demo the conditions are elaboration-time models; against real Chisel
they reference hardware signals and the same SVA scaffolding is produced.)

## Feature 3 — by-value relations

Relations name other specs **by value**, and `contFetchUnit` is declared at the
top of the file referencing nodes defined *below* it:

```scala
val contFetchUnit = spec {
  CONTRACT("CONT_FETCH_UNIT").desc("…")
    .has(intfEpmReqOut, intfEpmRespIn)          // ← forward references, by value
    .has(funcFetchRequest, funcFetchResponse)
    .uses(paramPcWidth, paramTxnId)
    .build()
}
```

The `spec { … }` macro rewrites each by-value reference to the referent's
declaration path before evaluating the builder, so the compile-time evaluation
never dereferences a not-yet-initialised sibling (which previously failed with an
opaque `null`). Two consequences:

- **The compiler verifies the graph.** Typo a reference and it does not compile
  (`./run-demo.sh --ref-drift`):

  ```
  FetchSpecs.scala:32: not found: value intfEpmRespInTYPO
  ```

  String ids would have compiled and only been caught later by the checker.
- The checker resolves the paths back to ids, so `SpecIndex.json` still records
  plain ids (`"has": ["INTF_EPM_REQ_OUT", …]`) and still flags anything that does
  not resolve as a dangling reference.

## The report (`SpecCheck`)

```
Implementation coverage (structural nodes ↔ @LocalSpec)
  bound:     5 / 5
  ✓ every structural node is anchored to RTL

Formal coverage (PROPERTY/COVERAGE ↔ assert/cover)
  enforced:   3 / 4
  UNENFORCED (declared but not bound to any check):
    ⚠ PROP_EPOCH_FLUSH_CLEAN  [PROPERTY]
  bindings:
    • PROP_NO_REQTABLE_OVERFLOW  ⇐  reqCount.-(respCount).<=(maxOutstanding)   (FetchUnit.scala:50)
    • PROP_IN_ORDER_RESPONSE     ⇐  respCount.<=(reqCount)                     (FetchUnit.scala:51)
    • COV_OUTPUT_BACKPRESSURE    ⇐  reqCount.>(respCount)                      (FetchUnit.scala:52)

Typed bundle completeness
    ⚠ BND_INSTR_SLOT leaves undeclared: valid

Dangling references (HARD)
  ✓ every reference resolves to a defined spec

RESULT: PASS (no hard violations)
```

Note the division of labour: **field-level type drift never reaches the report —
it is a compile error.** The report covers the things the compiler *can't* see:
completeness, implementation coverage, formal coverage, dangling references.

### Human-readable output

Alongside the machine-readable `SpecIndex.json` / `TagIndex.json` / `properties.sva`,
`SpecCheck` also writes **`SPEC.md`** — a self-contained document that renders
on GitHub with no extra tooling: a coverage summary, a **Mermaid diagram** of the
spec graph, and a per-node section with descriptions, relations as links, bundle
field tables, RTL anchors and formal bindings. A committed snapshot is in
[`SPEC.sample.md`](SPEC.sample.md).

### Checker options

```
SpecCheck <meta-dir> [<out-dir>] [--clock=NAME] [--reset=NAME] [--strict]
```

| flag        | effect                                                                  |
|-------------|-------------------------------------------------------------------------|
| `--clock=`  | clock signal used in `properties.sva` (default `clk`)                    |
| `--reset=`  | reset signal used in `properties.sva` (default `reset`)                  |
| `--strict`  | CI gate: exit non-zero on *warnings* too (unimplemented / unenforced / incomplete), not just dangling refs |

By default only **dangling references** fail the build; the soft warnings are
informational until a project opts into `--strict`.

## Mapping to Chisel

| demo (`minichisel`)      | real Chisel            |
|--------------------------|------------------------|
| `class Bundle`           | `chisel3.Bundle`       |
| `UInt(w)` / `Bool()`     | `chisel3.UInt`/`Bool`  |
| `class Module`           | `chisel3.Module`       |
| `assert(cond, msg)`      | `chisel3.assert`       |
| `cover(cond, msg)`       | `chisel3.cover`        |

`bundleSpec[T]`, `@LocalSpec`, and `assertProperty` are unchanged — swap the
shim for `import chisel3._` and the bundles/specs/bindings work as-is.
