# frontend-demo — typed bundles + formal connection

A small, self-contained example that exercises two enhancements to the spec
framework on a slice of a RISC-V-style instruction **fetch unit**:

1. **Typed bundles (타입화)** — a `BUNDLE` spec is bound to the *implementation
   type*, so spec↔RTL **field drift becomes a compile error** instead of a
   silent JSON diff.
2. **Formal connection (formal 연결)** — a `PROPERTY`/`COVERAGE` spec is bound to
   a real boolean check, so the index records *what* is enforced and the checker
   reports *which declared properties have no check at all*.

It is deliberately **Chisel-free**: a ~15-line shim (`minichisel`) stands in for
`chisel3` so the demo builds in seconds with no heavyweight dependencies. The
spec/design code is identical against real Chisel — see *Mapping to Chisel*
below.

## Run it

```bash
./run-demo.sh           # build, emit indices, print the compliance report
./run-demo.sh --drift   # rename an implementation field → spec fails to compile
```

(Needs a JDK and `sbt`; override the launcher with `SBT=/path/to/sbt`.)

## Layout

| module        | what it is                                              |
|---------------|---------------------------------------------------------|
| `types/`      | `minichisel` shim + bundle types + params (no specs)    |
| `specs/`      | the spec graph — typed `bundleSpec[T]`, FUNCTIONs, …     |
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

Completeness is checked against the type's members at compile time:
`bundle` emits a **compile warning** for undeclared fields (`BND_INSTR_SLOT`
omits `valid`); `bundleExact` makes the same omission a **compile error**.

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
