# stream-processor-s3 — the same design, on Scala 3

The Scala 3 counterpart of [`../stream-processor`](../stream-processor).

```bash
./run.sh
```

Chisel is published **only for Scala 2.13** (there is no `chisel_3` artefact), so
the design here runs on a small **Chisel-shaped shim** (`streamproc.hdl`) instead
of Chisel. What matters is that the **spec graph is the *same source*** — shared
verbatim from the Chisel project via `unmanagedSourceDirectories` — and the
framework's Scala 3 macros (`spec`, `bundle`, `param`, `localSpec`,
`assertProperty`) produce the **same spec graph and coverage**:

```
44 spec nodes · 24/24 RTL-anchored · 8/8 formal-bound · 4/4 bundles complete · 0 dangling
```

(see [`SPEC.sample.md`](SPEC.sample.md) — the same nodes and coverage table as the
Chisel build. The *binding* count differs (32 vs 64) only because this shim build
runs the design once, whereas the Chisel build also drives it through the
verilator sim + model testbenches, which record extra sim bindings.)

## What this proves

- The framework runs on **Scala 3** with the **same API and IDs** as Scala 2.
- A real mid-size spec graph is **portable across Scala versions** — only the
  HDL backend (Chisel vs shim) differs; the spec layer is shared.
- Differences are confined to the macro internals (documented in
  [`../scala3-demo`](../scala3-demo)): on Scala 3 most specs are emitted at run
  time (`param` is the exception — it emits at compile time on both versions) and
  tagging uses the `localSpec(spec)` method rather than the `@LocalSpec`
  annotation.

When Chisel ships a Scala 3 artefact, this shim is the only thing that would be
replaced — the specs and the framework integration stay exactly as they are.
