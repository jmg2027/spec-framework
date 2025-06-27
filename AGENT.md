# Codex Agent: Scala SBT Runner for spec-framework

This agent compiles, tests, or runs the Scala project located under `/workspace/spec-framework` using `sbt`. The project is configured to operate **offline**, using a preloaded dependency cache.

---

## 📁 Directory Layout

Codex clones the repository into:

```
/workspace/
├── .sbt-cache/              ← sbt caches stored here (populated in setup)
└── spec-framework/          ← actual sbt project root
    ├── build.sbt
    ├── project/
    └── src/
```

All `sbt` commands must be run from `/workspace/spec-framework`.

---

## 🔧 sbt Cache Configuration

To enable offline operation, sbt must be invoked with the following JVM properties:

| Property | Description |
|----------|-------------|
| `sbt.ivy.home` | Path to preloaded Ivy2 JAR cache |
| `sbt.global.base` | Path to global sbt config/cache |

These are set to:

```
-Dsbt.ivy.home=/workspace/.sbt-cache/.ivy2
-Dsbt.global.base=/workspace/.sbt-cache/.sbt
```

---

## ✅ Example Commands

All commands must be prefixed with `cd /workspace/spec-framework &&`.

### Compile

```bash
cd /workspace/spec-framework && \
    sbt -Dsbt.ivy.home=/workspace/.sbt-cache/.ivy2 \
        -Dsbt.global.base=/workspace/.sbt-cache/.sbt \
        compile
```

### Run Tests

```bash
cd /workspace/spec-framework && \
    sbt -Dsbt.ivy.home=/workspace/.sbt-cache/.ivy2 \
        -Dsbt.global.base=/workspace/.sbt-cache/.sbt \
        test
```

### Run Main

```bash
cd /workspace/spec-framework && \
    sbt -Dsbt.ivy.home=/workspace/.sbt-cache/.ivy2 \
        -Dsbt.global.base=/workspace/.sbt-cache/.sbt \
        run
```

---

⚠️ Constraints
- The Codex runtime environment has no internet access. All dependencies must be preloaded in setup.sh using `sbt update`.
- Any change to `build.sbt` or `plugins.sbt` requires a full environment rebuild to re-trigger setup.
- Commands will fail if a new dependency is introduced but not preloaded.

---

🛠 Agent Integration Notes
- Always `cd /workspace/spec-framework` before calling sbt.
- Use `--error` or `--no-colors` flags if machine-parsing output.
- Consider wrapping the sbt call in a shell script (e.g., `sbtw`) for reusability.

---

✅ Maintenance Tip

To regenerate `.sbt-cache`, update `build.sbt` locally and run:

```bash
sbt update
```

Then archive the caches:

```bash
tar czf sbt-cache.tar.gz ~/.ivy2 ~/.sbt
```

Upload and extract the archive to `/workspace/.sbt-cache/` in Codex.

---
