#!/usr/bin/env bash
# -----------------------------------------------------------------------------
#  End-to-end test of the sbt plugin from a real consumer build:
#  publish the framework locally, then let SpecPlugin aggregate this project's
#  compile-time artefacts and assert the outputs are correct.
#
#    ./run.sh         (needs a JDK and `sbt`; override with SBT=/path/to/sbt)
# -----------------------------------------------------------------------------
set -euo pipefail

SBT="${SBT:-sbt}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

echo "### 1/3  Publishing the framework locally (spec-core/macros 2.13, spec-core/plugin 2.12) …"
( cd "$ROOT" && "$SBT" "set ThisBuild / scalafmtOnCompile := false" \
    "++2.12.19" specCore/publishLocal specPlugin/publishLocal \
    "++2.13.12" specCore/publishLocal specMacros/publishLocal )

echo "### 2/3  Running SpecPlugin's exportSpecIndex in the consumer build …"
( cd "$HERE" && "$SBT" exportSpecIndex )

echo "### 3/3  Verifying outputs …"
cd "$HERE"
fail() { echo "FAIL: $1"; exit 1; }
for f in target/SpecIndex.json target/TagIndex.json target/properties.sva; do
  [ -s "$f" ] || fail "$f missing/empty"
done
# by-value relations must have been resolved to ids by the plugin (no @fqn left)
grep -q '@fqn' target/SpecIndex.json && fail "unresolved @fqn left in SpecIndex.json"
grep -q '"INTF_REQ_OUT"' target/SpecIndex.json || fail "relation not resolved to INTF_REQ_OUT"
grep -q 'ap_PROP_TXN_BOUNDED' target/properties.sva || fail "property assertion missing from properties.sva"
echo "PASS: SpecIndex.json / TagIndex.json / properties.sva produced, @fqn relations resolved."
