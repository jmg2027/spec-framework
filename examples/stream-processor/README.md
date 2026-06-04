# stream-processor — a mid-size Chisel design, fully spec-mirrored

A **configurable AXI-Stream packet-processing pipeline** written in real
**Chisel 7**, with a spec graph that mirrors the design one-to-one
(top / modules / bundles / parameters). It is the promotion-grade example: a
recognisable, modular, configurable IP — not a toy — wired end to end to the
spec framework, producing browsable artefacts.

```bash
./run.sh        # build + elaborate (FIRRTL) + aggregate → artifacts/
```

The compliance report comes out **41 spec nodes · 24/24 RTL-anchored ·
5/5 properties enforced · 4/4 bundles complete · 0 dangling** — see
[`SPEC.sample.md`](SPEC.sample.md) (renders on GitHub: Mermaid graph + tables).

## Architecture

```
            in ─▶ IngressBuffer ─▶ Framer ─▶ Classifier ─▶ (Shaper?) ─▶ EgressBuffer ─▶ out
                  elastic FIFO     SOP/EOP    table-driven   token       drop/redirect
                                   framing    verdict        bucket      + FIFO
```

| stage | responsibility | key spec property |
|---|---|---|
| `IngressBuffer` | elastic FIFO, backpressure | `PROP_NO_BEAT_LOSS` |
| `Framer` | derive SOP/EOP from TLAST | `PROP_SOP_EOP_BALANCED` |
| `Classifier` | TDEST → action (drop/redirect/prio), latched per packet | `PROP_ACTION_STABLE` |
| `Shaper` *(optional)* | token-bucket rate limit | `PROP_TOKEN_BOUNDED` |
| `EgressBuffer` | apply verdict, buffer output | `PROP_DROPPED_NOT_EMITTED` |
| `StreamProcessorTop` | wire the Decoupled pipeline | — |

**Configurable / modular / extensible** via `SPConfig`: datapath width, FIFO
depths, classifier table size, header size, and an `enableShaper` toggle that
*omits the shaper stage entirely* from the elaborated hardware. Adding a stage =
add a module + a spec file + one `<>` in the top; the checker immediately tells
you if you forgot to anchor or enforce it.

## Directory layout (design ↔ spec mirroring)

```
src/main/scala/streamproc/
  design/                         specs/
    SPConfig.scala                  params/ParamSpecs.scala        (PARAMETER ×8)
    Bundles.scala                   bundles/BundleSpecs.scala      (typed BUNDLE ×4)
    modules/IngressBuffer.scala     modules/IngressBufferSpecs.scala
    modules/Framer.scala            modules/FramerSpecs.scala
    modules/Classifier.scala        modules/ClassifierSpecs.scala
    modules/Shaper.scala            modules/ShaperSpecs.scala
    modules/EgressBuffer.scala      modules/EgressBufferSpecs.scala
    top/StreamProcessorTop.scala    top/TopSpecs.scala
  Elaborate.scala                 (emits FIRRTL + forces spec emission)
```

Each design module opens with `localSpec(contX)`, tags its ports with
`localSpec(intfX)`, and binds its invariant with
`assert(assertProperty(propX){ … }, "msg")`. The specs reference each other
**by value** (`.has(intfFramerIn)`), so a typo'd reference is a compile error,
and the typed `bundle[StreamBeat](…)(_.data, _.keep, …)` reads field names/types
straight from the Chisel bundle.

## Artefacts (`artifacts/`)

| file | audience |
|---|---|
| `SpecIndex.json` / `TagIndex.json` | tooling / machine parsing |
| `properties.sva` | formal/sim scaffolding (one assert/cover per property) |
| `SPEC.md` | humans — Mermaid graph, coverage, per-node detail + RTL anchors |
| `target/StreamProcessorTop.fir` | the elaborated FIRRTL (proves it is real Chisel) |

## Scala 2 / Scala 3

Chisel is published **only for Scala 2.13** (there is no `chisel_3` artefact), so
this real-Chisel build is Scala 2.13. The spec framework itself is cross-built to
Scala 3 — the same design/spec structure on a Chisel-shaped shim is in
[`../stream-processor-s3`](../stream-processor-s3), which builds and reports
identically on Scala 3.3.
