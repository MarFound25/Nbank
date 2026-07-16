#!/usr/bin/env bash
# Quality gate: fail if API coverage (from allure widgets/coverage.json) is below threshold.
# Usage: check-api-coverage.sh [coverage.json] [threshold_percent]
set -euo pipefail

FILE="${1:-allure-report/widgets/coverage.json}"
THRESHOLD="${2:-50}"

if [[ ! -f "$FILE" ]]; then
  echo "ERROR: coverage file not found: $FILE"
  exit 1
fi

PCT="$(jq -r '
  if type == "array" then
    (.[0].value // .[0].percents // 0)
  else
    (.percents // .coverage // .value // 0)
  end
' "$FILE")"

echo "API coverage: ${PCT}% (threshold: ${THRESHOLD}%)"
echo "Source: $FILE"
jq . "$FILE" || true

if ! awk -v p="$PCT" -v t="$THRESHOLD" 'BEGIN { exit !(p + 0 >= t + 0) }'; then
  echo "QUALITY GATE FAILED: API coverage ${PCT}% < ${THRESHOLD}%"
  exit 1
fi

echo "QUALITY GATE PASSED: API coverage ${PCT}% >= ${THRESHOLD}%"
