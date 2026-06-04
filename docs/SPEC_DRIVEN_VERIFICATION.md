# Spec-driven verification

The spec graph is not just documentation bound to RTL — it is the **single source
of the verification plan**. The same `PROPERTY` / `COVERAGE` spec objects drive
three things, and the results flow back into one report:

```
                         ┌──────────────────────────────────────────┐
                         │   PROPERTY / COVERAGE spec  (one node)     │
                         └───────────────┬──────────────────────────┘
        assertProperty/coverProperty     │      checkProperty/coverPoint
        (in the RTL)                      │      (in the testbench)
        ┌─────────────────────────────┐  │  ┌──────────────────────────────┐
        │ chisel3 assert / cover      │  │  │ framework.spec.Bench          │
        │  → properties.sva           │  │  │  → VerifIndex.json            │
        │  → spec_formal.sby (SymbiY) │  │  │  (pass/fail/cycle, coverage)  │
        └─────────────────────────────┘  │  └──────────────────────────────┘
                         └───────────────┴──────────────┐
                                                         ▼
                                   SpecCheck → SPEC.md / report:
                                   per node — bound? · sim passed? · covered? · formal?
```

## Simulation side

`framework.spec.Bench` turns a property into a runtime checker and a coverage
node into a cover point. The **same spec object** the RTL asserts is reused in
the testbench:

```scala
// RTL (Chisel):
assert(assertProperty(propTokenBounded) { tokens <= c.shaperBurst.U }, "…")

// Testbench (the same propTokenBounded):
bench.checkProperty(propTokenBounded) { model.tokensNow <= c.shaperBurst }
bench.coverPoint(covTokensDrained)    { model.tokensNow == 0 }
```

After the run, `bench.writeIndex()` emits `VerifIndex.json`; `SpecCheck` folds it
in so every property shows its sim verdict and every cover point its hit count:

```
Verification (spec-driven simulation)
  exercised:   8 / 8     (4000 cycles)
    PROP_TOKEN_BOUNDED      ✓ passed (4000 cy)
    COV_BACKPRESSURE        ✓ covered (974 hits)
    COV_TOKENS_DRAINED      ○ uncovered (0/4000)   ← a real coverage hole
```

It catches regressions tied to the node: `Testbench --inject-bug` breaks the
token cap and the report shows `PROP_TOKEN_BOUNDED ✗ FAILED @cycle 0`.

### Pluggable backend (real tools, swappable)

The checker is backend-agnostic — every result carries a `backend` tag and the
report shows it (`Verification (spec-driven, backend: …)`). The example ships two:

| backend | what runs | how |
|---|---|---|
| `model` | a pure-Scala reference model | `Testbench` |
| `verilator` | the **real elaborated DUT** under verilator (ChiselSim); the spec assertions emitted by `assertProperty` are checked by the simulator itself | `RealTestbench` |

`RealTestbench --inject-bug` breaks the token cap in the RTL; verilator fires the
embedded assertion and its `[PROP_TOKEN_BOUNDED]` message maps the failure back to
the spec → `PROP_TOKEN_BOUNDED ✗ FAILED @cycle …  (backend: verilator)`. Swapping
in another simulator (VCS, Treadle, …) is a new `backend` value, not a rewrite —
the `checkProperty`/`coverPoint`/`recordAssert` API is the same.

## Functional coverage — auto-derived (no hand-written plan)

Functional coverage is normally a hand-maintained list ("did this interface fire?
was this function exercised?"). Here it is **derived from the spec graph**: every
`INTERFACE` and `FUNCTION` spec is automatically an obligation. The testbench only
supplies the activity bit:

```scala
bench.autoFunctional(SpecRegistry.allSpecs) { id => model.fired.contains(id) }
```

and the report closes it against the specs you already wrote:

```
Functional coverage (auto-derived from FUNCTION/INTERFACE specs)
  exercised:  18 / 18    (no coverage plan hand-written — derived from the spec graph)
  ✓ every interface fired and every function ran
```

`SpecCheck` also emits `functional_coverage.sva` — one `cover property` stub per
interface/function, ready to bind to signals.

## Formal side

`SpecCheck` emits `properties.sva` (one `assert/cover property` per node) and a
`spec_formal.sby` SymbiYosys script that proves them against the elaborated
Verilog. They are scaffolding: `sby spec_formal.sby` runs the proof once yosys +
a solver are installed.

## Why this matters (methodology)

- **One definition, three uses.** A property/coverage point is declared once (the
  spec node) and reused by the RTL assertion, the simulation checker, and the
  formal harness. No re-statement, no drift between "the plan" and "the checks".
- **Coverage closure is visible in the spec.** Unhit cover points
  (`COV_TOKENS_DRAINED` above — unreachable because the bucket refills as fast as
  it drains) surface immediately, against the spec that asked for them.
- **One dashboard.** `SPEC.md` reports, per node: implemented? · enforced by a
  check? · passed in sim? · covered? — so verification status lives next to the
  spec, not in a separate tracker.
