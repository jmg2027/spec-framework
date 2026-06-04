#!/usr/bin/env bash
# -----------------------------------------------------------------------------
#  End-to-end proof that the framework's macros work on Scala 3:
#  publish the framework for 3.x, then compile (emits .tag), run (emits .spec at
#  run time — Scala 3 has no compile-time c.eval), and aggregate with SpecCheck.
#
#    ./run.sh        (needs a JDK and `sbt`; override with SBT=/path/to/sbt)
# -----------------------------------------------------------------------------
set -euo pipefail

SBT="${SBT:-sbt}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

echo "### 1/2  Publishing the framework for Scala 3 (spec-core_3, spec-macros_3) …"
( cd "$ROOT" && "$SBT" "set ThisBuild / scalafmtOnCompile := false" \
    "++3.3.4" specCore/publishLocal specMacros/publishLocal )

echo "### 2/2  compile (tags) → run (specs) → SpecCheck …"
rm -rf "$HERE"/target/*/resource_managed
( cd "$HERE" && "$SBT" "runMain consumer.Main" "runMain framework.spec.SpecCheck" )
# SpecCheck exits non-zero on dangling refs, so a clean run means PASS.
echo "### Scala 3 demo PASSED"
