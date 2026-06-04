#!/usr/bin/env bash
# -----------------------------------------------------------------------------
#  frontend-demo: typed bundles + formal connection, end to end.
#
#    ./run-demo.sh           build the spec graph + RTL, emit indices, run checker
#    ./run-demo.sh --drift   rename an implementation field and watch the SPEC
#                            fail to compile (field drift = compile error)
#
#  Requires a JDK and `sbt` on PATH (override with `SBT=/path/to/sbt`).
# -----------------------------------------------------------------------------
set -euo pipefail

SBT="${SBT:-sbt}"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
META="$HERE/spec-meta"
OUT="$HERE"
SBT_FLAGS=("set ThisBuild / scalafmtOnCompile := false")

cd "$ROOT"

if [[ "${1:-}" == "--drift" ]]; then
  TYPES="$HERE/types/src/main/scala/frontend/demo/types/Types.scala"
  echo "### Renaming FetchRequest.addr -> address in the implementation type …"
  cp "$TYPES" "$TYPES.bak"
  trap 'mv "$TYPES.bak" "$TYPES"; echo "### restored Types.scala"' EXIT
  sed -i 's/  val addr  = UInt(p.pcWidth)/  val address = UInt(p.pcWidth)/' "$TYPES"
  echo "### Recompiling the spec graph (it still declares .field(\"addr\", _.addr)) …"
  echo "### Expect a COMPILE ERROR in FetchSpecs.scala, not a silent JSON diff:"
  echo
  "$SBT" "${SBT_FLAGS[@]}" "demoSpecs/clean" "demoSpecs/compile" || true
  exit 0
fi

rm -rf "$META"; mkdir -p "$META"

echo "### 1/2  Compile types → specs → design"
echo "###      (every .spec is emitted at compile time; .tag from @LocalSpec/assertProperty)"
echo "### 2/2  Aggregate indices, run the compliance report, generate properties.sva"
echo
"$SBT" "${SBT_FLAGS[@]}" \
  demoTypes/clean demoSpecs/clean demoDesign/clean \
  demoDesign/compile \
  "specCore/runMain framework.spec.SpecCheck $META $OUT"

echo
echo "### Wrote $OUT/SpecIndex.json, $OUT/TagIndex.json, $OUT/properties.sva"
