# Anchoring RTL to specs: `@LocalSpec` vs `localSpec`

There are two ways to bind an RTL declaration to a spec. They overlap but are not
redundant — and there is a historical reason both exist.

## `@LocalSpec(spec)` — macro annotation

The original form. A whitebox macro annotation on a **declaration**:

```scala
@LocalSpec(contFetchUnit)          // anchors the whole module
class FetchUnit extends Module {
  @LocalSpec(intfEpmReqOut)        // anchors a port, non-intrusively
  val reqOut = IO(Decoupled(...))
}
```

Why it was a macro annotation:

1. **Declaration-attached & non-intrusive.** It sits on a `class` / `object` /
   `def` / `val` and records its exact name (`owner.name`) without touching the
   right-hand side. Module-level anchoring (`@LocalSpec(cont) class Foo`) reads
   declaratively and has no clean method equivalent (you can't wrap a class).
2. **Tagging un-annotatable statements.** Chisel `when` / `:=` / `switch` are not
   declarations, so you can't annotate them. The klase32 idiom put the annotation
   on a dummy val just above: `@LocalSpec(spec) val _t = ()` then the `when {…}`.
3. It returns the annottee unchanged — a pure side-effecting tag.

## `localSpec(spec)` / `localSpec(spec, decl)` — method

Added later as a method-form alternative to the annotation.

```scala
class FetchUnit extends Module {
  localSpec(contFetchUnit)                                    // statement: anchors the module
  val reqOut = localSpec(intfEpmReqOut, IO(Decoupled(...)))   // value form: anchors the port, returns it
  localSpec(funcFetchRequest)                                 // tags a function/region — no dummy val needed
}
```

It improves on the annotation in two ways: the statement form **supersedes the
dummy-val trick** (tag a `when`/`:=` region directly), and the value form gives
**per-declaration granularity** while threading the value through.

## Which to use

| | `@LocalSpec` | `localSpec` |
|---|---|---|
| class/module level | ✅ cleanest | ✅ (statement in body) |
| port / val | ✅ (no wrap) | ✅ value form `localSpec(s, decl)` |
| a `when`/`:=` region | dummy-val trick | ✅ direct statement |

**Recommendation:** use `@LocalSpec` for declaration-level anchoring and
`localSpec` for statement regions and the value form. Both emit the same
`.tag`, so a module can mix them and the checker treats them identically.
