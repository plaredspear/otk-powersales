#!/usr/bin/env bash
#
# ============================================================================
#  SF Object → CSV 추출 (DB 미접근, 운영 org 안전 — read-only)
# ============================================================================
#
# Salesforce org 의 SObject 데이터를 추출하여 DB 컬럼명 헤더의 CSV 로 저장한다.
# 산출 CSV 는 db-import.sh 의 입력으로 사용. 본 스크립트는 DB 에 일체 접근하지
# 않으므로 운영 (prod) org 에서도 안전하게 실행 가능.
#
# ── 동작 ────────────────────────────────────────────────────────────────────
#   1) entity-meta.py 로 entity 메타 추출 (@SFField/@Column 매핑)
#   2) sf sobject describe 로 SF org 에 실존하는 필드 목록 조회
#   3) (1) ∩ (2) 필드만 SOQL SELECT 절에 포함 — entity 정의에는 있으나 org 에는
#      없는 필드 (예: sandbox 미생성) 자동 skip
#   4) sf data export bulk (Bulk API 2.0) 단일 채널로 추출 — row 수 무관 동일
#      포맷 보장. sf data query 와의 quoting/공백 처리 미세 차이 회피.
#   5) 헤더를 SF API name → DB 컬럼명으로 변환하여 출력
#
# ── NULL / 빈 문자열 정책 ───────────────────────────────────────────────────
#   SF CLI CSV 는 NULL 과 빈 문자열을 모두 빈 셀로 출력하여 구분 불가능하다.
#   본 도구는 양쪽을 PostgreSQL NULL 로 통일한다 (db-import.sh 의 \copy 가
#   빈 셀을 NULL 로 적재). SF Apex 코드 조사 결과(2026-05-14) 두 값을 다른
#   분기로 라우팅하는 사례 없음 — `!= null && != ''` 형태로 동등 취급. 단,
#   SOQL `WHERE col != ''` 같은 패턴을 PG 로 직역 시 결과 집합이 달라지므로
#   신규 쿼리는 `IS NOT NULL AND <> ''` 로 명시 변환할 것.
#
# ── 사용법 ──────────────────────────────────────────────────────────────────
#   sf-export.sh <SObject_API_Name> [output_csv_path]
#
#   기본 출력 경로: backend/scripts/sf-migrate/export/<SObject>.csv
#
# ── 필수 환경변수 ───────────────────────────────────────────────────────────
#   SF_TARGET_ORG                  SF CLI 의 org alias 명
#                                  사전 등록:  sf org login web --alias <alias>
#                                  확인:       sf org list
#                                  설정 예:    export SF_TARGET_ORG=otk-sbx
#
# ── 실행 예시 ───────────────────────────────────────────────────────────────
#   export SF_TARGET_ORG=otk-sbx
#   backend/scripts/sf-migrate/sf-export.sh Account
#   # → backend/scripts/sf-migrate/export/Account.csv
#
#   # 별도 경로
#   backend/scripts/sf-migrate/sf-export.sh Account /tmp/account-2026-05.csv
#
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SOBJECT="${1:?SObject API name 필수 — 예: Account, Org__c, ProductBarcode__c}"
OUT_CSV="${2:-${SCRIPT_DIR}/export/${SOBJECT}.csv}"

# 사전 조건
for cmd in sf python3 jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: $cmd 가 설치되어 있지 않습니다." >&2
    exit 1
  fi
done
: "${SF_TARGET_ORG:?환경변수 SF_TARGET_ORG 가 설정되어 있지 않습니다. (예: export SF_TARGET_ORG=otk-sbx)}"

mkdir -p "$(dirname "$OUT_CSV")"

echo "==============================================="
echo " SF Export  org=$SF_TARGET_ORG  sobject=$SOBJECT"
echo " out=$OUT_CSV"
echo "==============================================="

# ── 1) entity 메타 추출 ──
META=$(python3 "$SCRIPT_DIR/lib/entity-meta.py" "$SOBJECT")
ENTITY_CLASS=$(echo "$META" | jq -r '.entity_class')

# ── 2) SF describe 로 실존 필드 조회 ──
echo "[sf-export:$SOBJECT] describe 호출"
DESCRIBE_JSON=$(sf sobject describe -s "$SOBJECT" --target-org "$SF_TARGET_ORG" --json)
EXISTING_FIELDS_JSON=$(echo "$DESCRIBE_JSON" | jq '[.result.fields[].name]')

# ── 3) entity 메타 필드 중 org 에 실존하는 것만 필터 ──
FILTERED_META=$(echo "$META" | jq --argjson existing "$EXISTING_FIELDS_JSON" '
  .fields_skipped = [.fields[] | select(.sf as $sf | $existing | index($sf) | not)]
  | .fields = [.fields[] | select(.sf as $sf | $existing | index($sf))]
')

SKIPPED_COUNT=$(echo "$FILTERED_META" | jq '.fields_skipped | length')
if [[ "$SKIPPED_COUNT" -gt 0 ]]; then
  echo "[sf-export:$SOBJECT] WARN: SF org 에 부재하는 필드 ${SKIPPED_COUNT}건 skip:"
  echo "$FILTERED_META" | jq -r '.fields_skipped[] | "    - " + .sf + " (DB 컬럼: " + .db + ")"'
fi

SF_FIELDS=$(echo "$FILTERED_META" | jq -r '[.fields[].sf] | join(", ")')
SOQL="SELECT ${SF_FIELDS} FROM ${SOBJECT}"

# ── 4) Bulk API 2.0 추출 ──
RAW_CSV="${OUT_CSV%.csv}.raw.csv"
sf data export bulk \
  --query "$SOQL" \
  --target-org "$SF_TARGET_ORG" \
  --output-file "$RAW_CSV" \
  --result-format csv \
  --wait 30

# ── 5) 헤더 변환 (SF API name → DB 컬럼명) ──
python3 - "$RAW_CSV" "$OUT_CSV" "$FILTERED_META" <<'PYEOF'
import csv, json, sys

in_path, out_path, meta_json = sys.argv[1:4]
meta = json.loads(meta_json)
fields = meta["fields"]

with open(in_path, newline="", encoding="utf-8") as fin, \
     open(out_path, "w", newline="", encoding="utf-8") as fout:
    reader = csv.reader(fin)
    try:
        header = next(reader)
    except StopIteration:
        sys.exit("ERROR: 입력 CSV 가 비어 있습니다.")

    idx_of = {n: i for i, n in enumerate(header)}
    missing = [f["sf"] for f in fields if f["sf"] not in idx_of]
    if missing:
        sys.stderr.write(f"WARN: SF 응답에 없는 필드 (skip): {missing}\n")
    out_indices = [(f["db"], idx_of.get(f["sf"])) for f in fields]

    writer = csv.writer(fout)
    writer.writerow([db for db, _ in out_indices])

    n = 0
    for row in reader:
        writer.writerow(["" if i is None or i >= len(row) else row[i] for _, i in out_indices])
        n += 1
    sys.stderr.write(f"[sf-export] rows={n} cols={len(out_indices)}\n")
PYEOF

rm -f "$RAW_CSV"

echo "[OK] sf-export 완료 → $OUT_CSV"
