#!/usr/bin/env bash
#
# SF CLI 로 SObject 데이터를 CSV 로 추출 (row 수 자동 분기).
#
# 본 스크립트는 phase1-load.sh 가 호출한다. 단독 호출 시 환경변수
# (SF_TARGET_ORG, SF_MIGRATE_BULK_THRESHOLD) 는 migrate.sh 의 상단 주석 참조.
#
# 사용:
#   extract.sh <SF_api_name> <SOQL_query_string> <output_csv_path>
#
# 동작:
#   1) sf data query 로 COUNT 조회
#   2) 5000건 이하 → sf data query (CSV)
#      5000건 초과 → sf data export bulk (Bulk API 2.0)
#
set -euo pipefail

SF_API="${1:?SF api name 필수}"
SOQL="${2:?SOQL 쿼리 필수}"
OUT_CSV="${3:?출력 CSV 경로 필수}"

THRESHOLD="${SF_MIGRATE_BULK_THRESHOLD:-5000}"

# ── 1) COUNT 조회 ──
COUNT_JSON=$(sf data query \
  --query "SELECT COUNT() FROM ${SF_API}" \
  --target-org "$SF_TARGET_ORG" \
  --json)
TOTAL=$(echo "$COUNT_JSON" | python3 -c 'import sys, json; print(json.load(sys.stdin)["result"]["totalSize"])')

echo "[extract:$SF_API] totalSize=$TOTAL  threshold=$THRESHOLD"

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

echo "[extract:$SF_API] csv=$OUT_CSV  lines=$(wc -l < "$OUT_CSV")"
