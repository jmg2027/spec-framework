#!/usr/bin/env bash
# Publish framework libraries and validate the design consumer. Keep user caches.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
SBT="${SBT:-sbt}"
run_sbt() {
  if [ -n "${SPEC_SBT_CACHE:-}" ]; then
    mkdir -p "$SPEC_SBT_CACHE/ivy" "$SPEC_SBT_CACHE/global"
    "$SBT" "-Dsbt.ivy.home=$SPEC_SBT_CACHE/ivy" \
      "-Dsbt.global.base=$SPEC_SBT_CACHE/global" "$@"
  else
    "$SBT" "$@"
  fi
}

echo "[publish] cleaning project outputs (preserving dependency caches)"
run_sbt clean
run_sbt "+specCore / publishLocal"
run_sbt "+specMacros / publishLocal"
run_sbt "project specPlugin" "publishLocal"
(cd design && run_sbt clean exportSpecIndex)
echo "[publish] done; check design/target/SpecIndex.json and TagIndex.json"
