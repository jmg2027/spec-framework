#!/usr/bin/env bash
# -----------------------------------------------------------------------------
#  Build the real-Chisel StreamProcessor, elaborate it to FIRRTL, and aggregate
#  its spec graph into the promotion artefacts (SpecIndex.json / TagIndex.json /
#  properties.sva / SPEC.md).
#
#    ./run.sh        (needs a JDK and `sbt`; override with SBT=/path/to/sbt)
# -----------------------------------------------------------------------------
set -euo pipefail

SBT="${SBT:-sbt}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
OUT="$HERE/artifacts"

echo "### 1/3  Publishing the framework locally (Scala 2.13) …"
( cd "$ROOT" && "$SBT" "set ThisBuild / scalafmtOnCompile := false" \
    "++2.13.12" specCore/publishLocal specMacros/publishLocal )

echo "### 2/3  Compile (emits .tag + .spec) and elaborate (emits FIRRTL) …"
rm -rf "$HERE"/target/*/resource_managed
( cd "$HERE" && "$SBT" "runMain streamproc.Elaborate" )

echo "### 3/3  Aggregate the spec graph into $OUT …"
META="$(find "$HERE/target" -type d -name spec-meta | head -1)"
mkdir -p "$OUT"
( cd "$HERE" && "$SBT" "runMain framework.spec.SpecCheck $META $OUT" )

echo
echo "### Artefacts → $OUT :"
ls -1 "$OUT"
echo "### FIRRTL → $HERE/target/StreamProcessorTop.fir"
