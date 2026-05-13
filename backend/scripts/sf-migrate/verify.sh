#!/usr/bin/env bash
#
# 검증 (entity-driven):
#   1) 적재 행수 (수기 대조용 출력)
#   2) sfid NULL 잔존 = 0
#   3) Orphan FK = 0 (target 이 범위 내일 때만 — 범위 외 FK 는 NULL 정상)
#
# 사용:
#   verify.sh <SObject1> [SObject2 ...]
#
# exit 1 if any failure.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${SF_MIGRATE_META:?entity-meta.py 경로 필요}"

if [[ $# -eq 0 ]]; then
  echo "ERROR: SObject API name 인자가 비어 있습니다." >&2
  exit 1
fi

SOBJECTS=("$@")
SCHEMA="powersales"

declare -A TABLE_IN_SCOPE
for sobj in "${SOBJECTS[@]}"; do
  table=$(python3 "$SF_MIGRATE_META" "$sobj" | jq -r '.table')
  TABLE_IN_SCOPE[$table]=1
done

FAIL=0

echo ""
echo "── [Verify] 시작"

for sobj in "${SOBJECTS[@]}"; do
  meta=$(python3 "$SF_MIGRATE_META" "$sobj")
  ENTITY_CLASS=$(echo "$meta" | jq -r '.entity_class')
  TABLE=$(echo "$meta" | jq -r '.table')

  CNT=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TABLE};")
  SFID_NULL=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TABLE} WHERE sfid IS NULL;")

  printf "  %-30s rows=%-8s sfidNull=%s" "$TABLE" "$CNT" "$SFID_NULL"

  if [[ "$SFID_NULL" -ne 0 ]]; then
    printf "  [FAIL: sfid NULL]"
    FAIL=1
  fi

  # FK 매핑 중 target 이 범위 내인 것만 orphan 체크
  while IFS=$'\t' read -r SRC_SFID SRC_FK TARGET_TABLE; do
    [[ -z "$SRC_SFID" ]] && continue
    if [[ -z "${TABLE_IN_SCOPE[$TARGET_TABLE]:-}" ]]; then
      continue
    fi
    ORPHAN=$(psql -At -c "
      SELECT COUNT(*) FROM ${SCHEMA}.${TABLE}
      WHERE ${SRC_SFID} IS NOT NULL AND ${SRC_FK} IS NULL;
    ")
    printf "  orphan(%s)=%s" "$SRC_FK" "$ORPHAN"
    if [[ "$ORPHAN" -ne 0 ]]; then
      printf " [FAIL]"
      FAIL=1
    fi
  done < <(echo "$meta" | jq -r '.fk_mappings[] | [.src_sfid, .src_fk, .target_table] | @tsv')

  echo ""
done

echo ""
if [[ "$FAIL" -ne 0 ]]; then
  echo "[FAIL] verify 실패 — 위 출력 확인"
  exit 1
fi
echo "[OK] verify 통과"
