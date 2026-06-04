#!/usr/bin/env bash
# Generate a Chisel design skeleton FROM the spec graph (spec → design).
#   ./gen.sh [out-dir]   (default: ./gen-out)   — needs the spec graph emitted first
set -euo pipefail
SBT="${SBT:-sbt}"; HERE="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$HERE/../.." && pwd)"
OUT="${1:-$HERE/gen-out}"
( cd "$ROOT" && "$SBT" "set ThisBuild / scalafmtOnCompile := false" "++2.13.12" specCore/publishLocal specMacros/publishLocal )
rm -rf "$HERE"/target/*/resource_managed
( cd "$HERE" && "$SBT" "runMain streamproc.Elaborate" "runMain streamproc.WidthProbe" )
META="$(find "$HERE/target" -type d -name spec-meta | head -1)"
( cd "$HERE" && "$SBT" "runMain framework.spec.SpecGen $META $OUT" )
echo "### Generated skeleton → $OUT/Generated.scala"
