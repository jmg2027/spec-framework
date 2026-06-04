#!/usr/bin/env bash
# Scala 3 counterpart of stream-processor (Chisel-shaped shim; specs shared).
set -euo pipefail
SBT="${SBT:-sbt}"; HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"; OUT="$HERE/artifacts"
echo "### Publishing the framework for Scala 3 …"
( cd "$ROOT" && "$SBT" "set ThisBuild / scalafmtOnCompile := false" "++3.3.4" specCore/publishLocal specMacros/publishLocal )
echo "### compile (tags) + run (emits specs) + aggregate …"
rm -rf "$HERE"/target/*/resource_managed
( cd "$HERE" && "$SBT" "runMain streamproc.design.top.Main" )
META="$(find "$HERE/target" -type d -name spec-meta | head -1)"; mkdir -p "$OUT"
( cd "$HERE" && "$SBT" "runMain framework.spec.SpecCheck $META $OUT" )
echo "### Artefacts → $OUT :"; ls -1 "$OUT"
