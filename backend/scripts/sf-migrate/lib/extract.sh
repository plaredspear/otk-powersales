#!/usr/bin/env bash
#
# SF CLI 로 SObject 데이터를 CSV 로 추출 (row 수 자동 분기).
#
# 사용:
#   extract.sh <entity_name> <sf_api_name> <output_csv_path>
#
# 동작:
#   1) sf data query 로 COUNT 조회
#   2) 5000건 이하 → sf data query (CSV)
#      5000건 초과 → sf data export bulk (Bulk API 2.0)
#   3) SELECT 절은 select/<entity>.soql 사이드카에서 읽음
#
set -euo pipefail

ENTITY="${1:?entity 명 필수}"
SF_API="${2:?SF api name 필수}"
OUT_CSV="${3:?출력 CSV 경로 필수}"

THRESHOLD="${SF_MIGRATE_BULK_THRESHOLD:-5000}"
SOQL_FILE="${SF_MIGRATE_DIR}/select/${ENTITY}.soql"

if [[ ! -f "$SOQL_FILE" ]]; then
  echo "ERROR: SOQL sidecar 가 없습니다: $SOQL_FILE" >&2
  exit 1
fi

SOQL="$(tr -d '\n' < "$SOQL_FILE" | sed 's/[[:space:]]\+/ /g')"

# ── 1) COUNT 조회 ──
COUNT_JSON=$(sf data query \
  --query "SELECT COUNT() FROM ${SF_API}" \
  --target-org "$SF_TARGET_ORG" \
  --json)
TOTAL=$(echo "$COUNT_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["result"]["totalSize"])')

echo "[extract:$ENTITY] totalSize=$TOTAL  threshold=$THRESHOLD"

# ── 2) 추출 ──
if [[ "$TOTAL" -le "$THRESHOLD" ]]; then
  sf data query \
    --query "$SOQL" \
    --target-org "$SF_TARGET_ORG" \
    --result-format csv \
    > "$OUT_CSV"
else
  sf data export bulk \
    --query "$SOQL" \
    --target-org "$SF_TARGET_ORG" \
    --output-file "$OUT_CSV" \
    --result-format csv \
    --wait 30
fi

echo "[extract:$ENTITY] csv=$OUT_CSV  lines=$(wc -l < "$OUT_CSV")"
