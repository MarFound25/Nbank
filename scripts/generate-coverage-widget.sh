#!/usr/bin/env bash
# Build allure-report/widgets/coverage.json from swagger-coverage-results.json
set -euo pipefail

RESULTS="${1:-swagger-coverage-results.json}"
OUT="${2:-allure-report/widgets/coverage.json}"

mkdir -p "$(dirname "$OUT")"

if [[ ! -f "$RESULTS" ]]; then
  echo "WARN: $RESULTS not found — writing 0% coverage widget"
  cat > "$OUT" <<'EOF'
{
  "percents": 0,
  "covered": 0,
  "uncovered": 0,
  "total": 0,
  "full": 0,
  "partial": 0,
  "empty": 0
}
EOF
  exit 0
fi

jq '
  .coverageOperationMap.counter as $c
  | (if $c.all == 0 then 0 else (($c.full + $c.party) / $c.all * 100) end) as $pct
  | {
      percents: (($pct * 100 | round) / 100),
      covered: ($c.full + $c.party),
      uncovered: $c.empty,
      total: $c.all,
      full: $c.full,
      partial: $c.party,
      empty: $c.empty,
      conditions: {
        covered: .conditionCounter.covered,
        all: .conditionCounter.all
      }
    }
' "$RESULTS" > "$OUT"

echo "Wrote coverage widget: $OUT"
cat "$OUT"
