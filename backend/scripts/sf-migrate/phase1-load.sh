#!/usr/bin/env bash
#
# Phase 1: SF → DB 적재.
#
# 1) 대상 테이블의 FK + NOT NULL constraint 백업 → restore.sql
# 2) constraint 임시 DROP
# 3) 각 entity 별:
#    - TRUNCATE
#    - SF CLI 추출 (row 수 분기)
#    - CSV transform (헤더 SF→DB)
#    - psql \copy 적재
# 4) (constraint 복원은 phase2 후로 미룸)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${SF_MIGRATE_WORK_DIR:?상위에서 호출되어야 합니다}"

# parse list
TARGET_TABLES=()
TARGET_LINES=()
while IFS= read -r line; do
  IFS='|' read -r ENTITY SF_API TABLE FK_MAP <<<"$line"
  TARGET_TABLES+=("$TABLE")
  TARGET_LINES+=("$line")
done < <("$SCRIPT_DIR/lib/parse-list.sh")

if [[ ${#TARGET_TABLES[@]} -eq 0 ]]; then
  echo "ERROR: 대상 entity 가 비어 있습니다 (sobject-list.txt 또는 --only 필터 확인)." >&2
  exit 1
fi

echo ""
echo "── [Phase 1] 시작 — 대상: ${TARGET_TABLES[*]}"

# ── 1) constraint 백업 ──
RESTORE_SQL="${SF_MIGRATE_WORK_DIR}/restore.sql"
"$SCRIPT_DIR/lib/constraints.sh" save "$RESTORE_SQL" "${TARGET_TABLES[@]}"

# ── 2) constraint DROP ──
"$SCRIPT_DIR/lib/constraints.sh" drop "${TARGET_TABLES[@]}"

# ── 3) entity 별 적재 ──
for line in "${TARGET_LINES[@]}"; do
  IFS='|' read -r ENTITY SF_API TABLE FK_MAP <<<"$line"

  echo ""
  echo "── [Phase 1] $ENTITY ($SF_API → $TABLE)"

  # 3-1) TRUNCATE
  psql -v ON_ERROR_STOP=1 -q -c "TRUNCATE TABLE powersales.${TABLE} RESTART IDENTITY CASCADE;"

  # 3-2) 추출
  RAW_CSV="${SF_MIGRATE_WORK_DIR}/${ENTITY}.raw.csv"
  "$SCRIPT_DIR/lib/extract.sh" "$ENTITY" "$SF_API" "$RAW_CSV"

  # 3-3) 변환
  OUT_CSV="${SF_MIGRATE_WORK_DIR}/${ENTITY}.csv"
  DB_COLS="$("$SCRIPT_DIR/lib/transform.sh" "$ENTITY" "$RAW_CSV" "$OUT_CSV")"

  # 3-4) 적재
  psql -v ON_ERROR_STOP=1 -q -c "\copy powersales.${TABLE}(${DB_COLS}) FROM '${OUT_CSV}' WITH (FORMAT csv, HEADER true)"

  # 3-5) post-load (선택) — 파생 컬럼 등 SF 매핑 외 컬럼 채움
  POST_SQL="${SCRIPT_DIR}/post-load/${ENTITY}.sql"
  if [[ -f "$POST_SQL" ]]; then
    psql -v ON_ERROR_STOP=1 -q -f "$POST_SQL"
    echo "[Phase 1:$ENTITY] post-load 적용 ($(basename "$POST_SQL"))"
  fi

  LOADED=$(psql -At -c "SELECT COUNT(*) FROM powersales.${TABLE};")
  echo "[Phase 1:$ENTITY] loaded=$LOADED"
done

echo ""
echo "[OK] Phase 1 완료. restore.sql=$RESTORE_SQL (constraint 는 Phase 2 후 복원)"
