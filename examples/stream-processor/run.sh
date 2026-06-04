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

echo "### 2/4  Elaborate (FIRRTL) + capture bundle widths + spec-driven testbench …"
rm -rf "$HERE"/target/*/resource_managed
( cd "$HERE" && "$SBT" "runMain streamproc.Elaborate" "runMain streamproc.WidthProbe" "runMain streamproc.verif.Testbench" )

echo "### 3/4  REAL verilator simulation of the same spec assertions (ChiselSim) …"
#   The verilator run proves the spec assertions on the actual RTL; add --inject-bug
#   to watch PROP_TOKEN_BOUNDED fire. (It writes a verilator-backend VerifIndex; we
#   keep the richer model VerifIndex for the report, so re-run the model TB after.)
( cd "$HERE" && "$SBT" "runMain streamproc.verif.RealTestbench" "runMain streamproc.verif.Testbench" )

echo "### 4/4  Aggregate the spec graph + verification results into $OUT …"
META="$(find "$HERE/target" -type d -name spec-meta | head -1)"
mkdir -p "$OUT"
( cd "$HERE" && "$SBT" "runMain framework.spec.SpecCheck $META $OUT" )

echo
echo "### Artefacts → $OUT :"
ls -1 "$OUT"
echo "### FIRRTL → $HERE/target/StreamProcessorTop.fir"
