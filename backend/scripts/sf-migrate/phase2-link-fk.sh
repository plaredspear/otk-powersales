#!/usr/bin/env bash
#
# Phase 2: *_sfid → 참조 entity 의 sfid → 로컬 PK 로 FK UPDATE (entity-driven).
#
# 본 스크립트는 migrate.sh 가 호출한다. 단독 호출 시 환경변수
# (DEV_OTK_PWRS_DB_PASSWORD, SF_MIGRATE_WORK_DIR 등) 는
# migrate.sh 의 상단 주석 참조.
#
# 입력: SObject API name 인자 리스트.
# 동작:
#   - 각 sobject 의 entity-meta.py 결과에서 fk_mappings 배열 추출
#   - 매핑 1건당 UPDATE 1개:
#       UPDATE <schema>.<table> AS src
#       SET <src_fk> = tgt.<target_pk>
#       FROM <schema>.<target_table> AS tgt
#       WHERE src.<src_sfid> = tgt.sfid;
#   - target_table 의 PK 컬럼명은 pg_index/pg_attribute 동적 조회
#   - target 테이블 이 본 실행 범위에 없으면 skip (orphan 으로 남음, verify 가 보고)
#
# 마지막에 Phase 1 백업 SQL 로 constraint 복원.
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
SCHEMA="powersales"

echo ""
echo "── [Phase 2] 시작 — FK 연결 대상 sobject (${#SOBJECTS[@]}): ${SOBJECTS[*]}"

# 본 실행 범위에 포함된 table 목록 (FK target 가 범위 외이면 skip 판단용)
declare -A TABLE_IN_SCOPE
for sobj in "${SOBJECTS[@]}"; do
  table=$(python3 "$SF_MIGRATE_META" "$sobj" | jq -r '.table')
  TABLE_IN_SCOPE[$table]=1
done

UPDATE_COUNT=0
SKIP_COUNT=0

for sobj in "${SOBJECTS[@]}"; do
  meta=$(python3 "$SF_MIGRATE_META" "$sobj")
  ENTITY_CLASS=$(echo "$meta" | jq -r '.entity_class')
  TABLE=$(echo "$meta" | jq -r '.table')
  FK_COUNT=$(echo "$meta" | jq '.fk_mappings | length')

  if [[ "$FK_COUNT" -eq 0 ]]; then
    continue
  fi

  while IFS=$'\t' read -r SRC_SFID SRC_FK TARGET_TABLE TARGET_CLASS; do
    if [[ -z "${TABLE_IN_SCOPE[$TARGET_TABLE]:-}" ]]; then
      echo "[Phase 2:$ENTITY_CLASS] skip ${TABLE}.${SRC_FK} (target ${TARGET_TABLE} 가 범위 외)"
      SKIP_COUNT=$((SKIP_COUNT + 1))
      continue
    fi

    TARGET_PK=$(psql -At -c "
      SELECT a.attname
      FROM pg_index i
      JOIN pg_class c ON c.oid = i.indrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
      JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
      WHERE i.indisprimary
        AND n.nspname = '${SCHEMA}'
        AND c.relname = '${TARGET_TABLE}';
    ")
    if [[ -z "$TARGET_PK" ]]; then
      echo "ERROR: target ${TARGET_TABLE} 의 PK 를 찾을 수 없습니다." >&2
      exit 1
    fi

    echo "[Phase 2:$ENTITY_CLASS] ${TABLE}.${SRC_FK} ← ${TARGET_TABLE}.${TARGET_PK} (via ${SRC_SFID} → ${TARGET_TABLE}.sfid)"

    psql -v ON_ERROR_STOP=1 -At -c "
      UPDATE ${SCHEMA}.${TABLE} AS src
      SET ${SRC_FK} = tgt.${TARGET_PK}
      FROM ${SCHEMA}.${TARGET_TABLE} AS tgt
      WHERE src.${SRC_SFID} = tgt.sfid;
    " > /dev/null
    UPDATE_COUNT=$((UPDATE_COUNT + 1))
  done < <(echo "$meta" | jq -r '.fk_mappings[] | [.src_sfid, .src_fk, .target_table, .target_class] | @tsv')
done

echo ""
echo "[Phase 2] FK UPDATE 처리: $UPDATE_COUNT 건 실행, $SKIP_COUNT 건 skip"

# ── constraint 복원 ──
RESTORE_SQL="${SF_MIGRATE_WORK_DIR}/restore.sql"
if [[ -f "$RESTORE_SQL" ]]; then
  "$SCRIPT_DIR/lib/constraints.sh" restore "$RESTORE_SQL"
else
  echo "WARN: restore.sql 이 없습니다 — Phase 1 을 단독 실행하지 않았는지 확인 (${RESTORE_SQL})"
fi

echo "[OK] Phase 2 완료"
