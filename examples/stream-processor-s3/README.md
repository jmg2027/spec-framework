# stream-processor-s3 — the same design, on Scala 3

The Scala 3 counterpart of [`../stream-processor`](../stream-processor).

```bash
./run.sh
```

Chisel is published **only for Scala 2.13** (there is no `chisel_3` artefact), so
the design here runs on a small **Chisel-shaped shim** (`streamproc.hdl`) instead
of Chisel. What matters is that the **spec graph is the *same source*** — shared
verbatim from the Chisel project via `unmanagedSourceDirectories` — and the
framework's Scala 3 macros (`spec`, `bundle`, `localSpec`, `assertProperty`)
produce an **identical** compliance report:

```
41 spec nodes · 24/24 RTL-anchored · 5/5 properties enforced · 4/4 bundles complete · 0 dangling
```

(see [`SPEC.sample.md`](SPEC.sample.md), byte-for-byte the same coverage as the
Chisel build).

## What this proves

- The framework runs on **Scala 3** with the **same API and IDs** as Scala 2.
- A real mid-size spec graph is **portable across Scala versions** — only the
  HDL backend (Chisel vs shim) differs; the spec layer is shared.
- Differences are confined to the macro internals (documented in
  [`../scala3-demo`](../scala3-demo)): on Scala 3 specs are emitted at run time
  and tagging uses the `localSpec(spec)` method rather than the `@LocalSpec`
  annotation.

When Chisel ships a Scala 3 artefact, this shim is the only thing that would be
replaced — the specs and the framework integration stay exactly as they are.
