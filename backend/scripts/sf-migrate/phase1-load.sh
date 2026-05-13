#!/usr/bin/env bash
#
# Phase 1: SF → DB 적재 (entity-driven).
#
# 본 스크립트는 migrate.sh 가 호출한다. 단독 호출 시 환경변수
# (SF_TARGET_ORG, DEV_OTK_PWRS_DB_PASSWORD, SF_MIGRATE_WORK_DIR 등) 는
# migrate.sh 의 상단 주석 참조.
#
# 입력: 인자로 SObject API name 리스트.
# 동작:
#   1) entity-meta.py 로 모든 sobject 의 메타 수집 → 대상 테이블 목록 산출
#   2) 대상 테이블의 FK + NOT NULL constraint 임시 DROP + 복원 SQL 백업
#   3) 각 sobject 별 loop:
#      - TRUNCATE <table>
#      - SOQL 동적 생성 (entity 의 @SFField 목록 + Id + CreatedDate + LastModifiedDate)
#      - SF CLI 추출
#      - CSV transform (SF→DB 컬럼명 변환, 메타 기반)
#      - psql \copy 적재
#      - post-load/<entity_class>.sql 존재 시 적용
#   4) constraint 복원은 phase2 후로 미룸
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${SF_MIGRATE_WORK_DIR:?상위에서 호출되어야 합니다}"
: "${SF_MIGRATE_META:?entity-meta.py 경로 필요}"

if [[ $# -eq 0 ]]; then
  echo "ERROR: SObject API name 인자가 비어 있습니다." >&2
  exit 1
fi

SOBJECTS=("$@")

echo ""
echo "── [Phase 1] 시작 — 대상 sobject (${#SOBJECTS[@]}): ${SOBJECTS[*]}"

# ── 1) 메타 수집 ──
declare -A META_BY_SOBJECT
TABLES=()
for sobj in "${SOBJECTS[@]}"; do
  meta=$(python3 "$SF_MIGRATE_META" "$sobj")
  META_BY_SOBJECT[$sobj]="$meta"
  TABLES+=("$(echo "$meta" | jq -r '.table')")
done

# ── 2) constraint 백업 + DROP ──
RESTORE_SQL="${SF_MIGRATE_WORK_DIR}/restore.sql"
"$SCRIPT_DIR/lib/constraints.sh" save "$RESTORE_SQL" "${TABLES[@]}"
"$SCRIPT_DIR/lib/constraints.sh" drop "${TABLES[@]}"

# ── 3) 각 sobject 적재 ──
for sobj in "${SOBJECTS[@]}"; do
  meta="${META_BY_SOBJECT[$sobj]}"
  ENTITY_CLASS=$(echo "$meta" | jq -r '.entity_class')
  TABLE=$(echo "$meta" | jq -r '.table')
  SCHEMA=$(echo "$meta" | jq -r '.schema')

  # SF 필드 목록 (SELECT 절)
  SF_FIELDS=$(echo "$meta" | jq -r '[.fields[].sf] | join(", ")')
  SOQL="SELECT ${SF_FIELDS} FROM ${sobj}"

  echo ""
  echo "── [Phase 1] $ENTITY_CLASS ($sobj → ${SCHEMA}.${TABLE})"

  # 3-1) TRUNCATE
  psql -v ON_ERROR_STOP=1 -q -c "TRUNCATE TABLE ${SCHEMA}.${TABLE} RESTART IDENTITY CASCADE;"

  # 3-2) 추출
  RAW_CSV="${SF_MIGRATE_WORK_DIR}/${ENTITY_CLASS}.raw.csv"
  "$SCRIPT_DIR/lib/extract.sh" "$sobj" "$SOQL" "$RAW_CSV"

  # 3-3) 변환
  OUT_CSV="${SF_MIGRATE_WORK_DIR}/${ENTITY_CLASS}.csv"
  DB_COLS=$("$SCRIPT_DIR/lib/transform.sh" "$RAW_CSV" "$OUT_CSV" "$meta")

  # 3-4) 적재
  psql -v ON_ERROR_STOP=1 -q -c "\copy ${SCHEMA}.${TABLE}(${DB_COLS}) FROM '${OUT_CSV}' WITH (FORMAT csv, HEADER true)"

  # 3-5) post-load (선택) — SF 매핑 외 파생 컬럼 채움
  POST_SQL="${SCRIPT_DIR}/post-load/${ENTITY_CLASS}.sql"
  if [[ -f "$POST_SQL" ]]; then
    psql -v ON_ERROR_STOP=1 -q -f "$POST_SQL"
    echo "[Phase 1:$ENTITY_CLASS] post-load 적용 ($(basename "$POST_SQL"))"
  fi

  LOADED=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TABLE};")
  echo "[Phase 1:$ENTITY_CLASS] loaded=$LOADED"
done

echo ""
echo "[OK] Phase 1 완료. restore.sql=$RESTORE_SQL (constraint 는 Phase 2 후 복원)"
