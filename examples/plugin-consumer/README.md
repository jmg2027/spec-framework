# plugin-consumer — end-to-end SpecPlugin test

A standalone, single-module sbt build that consumes the **published** framework
the way a real project would, and drives the sbt plugin end to end:

```bash
./run.sh        # publishLocal the framework, run exportSpecIndex, verify outputs
```

(Or, once the framework is `publishLocal`-ed: `sbt exportSpecIndex`.)

It exercises the whole pipeline through the plugin (not the `SpecCheck` CLI):

- typed bundle `bundle[MemReq](…)(_.addr, _.txnId)`,
- a **by-value relation** `.has(intfReqOut)` declared *before* the interface, so
  the plugin must resolve the `@fqn:` reference back to `INTF_REQ_OUT`,
- `@LocalSpec` tags on the module/port,
- a bound property `assertProperty(propBounded){ … }` → `properties.sva`.

`run.sh` then asserts that `SpecIndex.json`, `TagIndex.json` and `properties.sva`
are produced, that no unresolved `@fqn` reference leaks into the index, and that
the relation resolved to `INTF_REQ_OUT`.

## Why a single module

`SpecPlugin` aggregates one project's `resourceManaged/spec-meta`, so the
plugin's home turf is a single-project consumer. Multi-module spec graphs (where
specs and design live in different modules sharing a meta directory) are better
served by the `framework.spec.SpecCheck` CLI — see
[`../frontend-demo`](../frontend-demo), which aggregates a shared directory.
