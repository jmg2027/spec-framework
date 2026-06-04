# scala3-demo — the framework on Scala 3

The same spec graph + fetch-unit-style design as
[`plugin-consumer`](../plugin-consumer), written for **Scala 3.3**. It proves the
macros work on Scala 3 and produce the same compliance report as the Scala 2
path.

```bash
./run.sh        # publish the framework for 3.x, compile, run, check
```

## What is identical to Scala 2

The user-facing API is the same: `spec { … }`, `bundle[T](…)(_.f, …)`,
`assertProperty(p){ cond }`, by-value relations (`.has(intfReqOut)`), and the
exact same `SpecCheck` report (5 nodes, relations resolved, formal coverage,
bundle completeness, dangling refs).

## What differs on Scala 3 (and why)

| concern | Scala 2 | Scala 3 |
|---|---|---|
| spec `.spec` emission | **compile time** (the `spec` macro evaluates the builder via `c.eval`) | **run time** — Scala 3 has no `c.eval`, so the macro returns code that builds + emits the spec when the object initialises (e.g. during elaboration). The demo's `Main` forces this. |
| tagging RTL | `@LocalSpec(spec)` **annotation** | `localSpec(spec)` **method** — Scala 3 macro annotations are experimental and (in 3.3) crash the inliner on macro-produced `val` arguments, so an equivalent inline method is used instead. |
| relation / tag ids | resolved to real ids at compile time | recorded as `@fqn:<path>` and resolved by `SpecCheck` at aggregation |

Because specs are emitted at run time, the flow is **compile → run → check**
(the `SpecCheck` CLI on the shared meta directory), not the post-compile sbt
plugin. The output indices are identical to the Scala 2 ones.

## Architecture

The macros are the only version-specific code: `spec-macros/src/main/scala-2`
holds the Scala 2 def-macros and the `@LocalSpec` annotation; `…/scala-3` holds
the inline+quotes versions. Everything else — the data model, the DSL, and
`SpecCheck` (`spec-core`) — is shared and cross-builds to 2.12, 2.13 and 3.
