#!/usr/bin/env bash
#
# Phase 2: *_sfid 컬럼 → 참조 entity 의 sfid → 로컬 PK 로 FK UPDATE.
#
# FK 매핑 표기 (sobject-list.txt): <src_sfid>-><target>.sfid-><src_fk>
#   여러 FK 는 ';' 로 구분.
#
# Phase 2 마지막에 Phase 1 에서 백업한 restore.sql 로 constraint 복원.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${SF_MIGRATE_WORK_DIR:?상위에서 호출되어야 합니다}"

echo ""
echo "── [Phase 2] 시작 — FK 연결"

UPDATE_COUNT=0

while IFS= read -r line; do
  IFS='|' read -r ENTITY SF_API TABLE FK_MAP <<<"$line"

  if [[ -z "${FK_MAP:-}" || "${FK_MAP// }" == "" ]]; then
    continue
  fi

  IFS=';' read -ra MAPS <<<"$FK_MAP"
  for m in "${MAPS[@]}"; do
    # 형식: <src_sfid>-><target>.sfid-><src_fk>
    m_clean="$(echo "$m" | tr -d ' ')"
    if [[ ! "$m_clean" =~ ^([a-zA-Z_]+)-\>([a-zA-Z_]+)\.sfid-\>([a-zA-Z_]+)$ ]]; then
      echo "ERROR: FK 매핑 형식 오류 ($ENTITY): $m" >&2
      exit 1
    fi
    SRC_SFID="${BASH_REMATCH[1]}"
    TARGET_TABLE="${BASH_REMATCH[2]}"
    SRC_FK="${BASH_REMATCH[3]}"

    # target 테이블의 PK 컬럼명 동적 조회 (단일 컬럼 PK 가정 — IDENTITY)
    TARGET_PK=$(psql -At -c "
      SELECT a.attname
      FROM pg_index i
      JOIN pg_class c ON c.oid = i.indrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
      JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
      WHERE i.indisprimary
        AND n.nspname = 'powersales'
        AND c.relname = '${TARGET_TABLE}';
    ")

    if [[ -z "$TARGET_PK" ]]; then
      echo "ERROR: target 테이블 ${TARGET_TABLE} 의 PK 를 찾을 수 없습니다." >&2
      exit 1
    fi

    echo "[Phase 2:$ENTITY] ${TABLE}.${SRC_FK} ← ${TARGET_TABLE}.${TARGET_PK} (via ${SRC_SFID} → ${TARGET_TABLE}.sfid)"

    UPDATE_RESULT=$(psql -v ON_ERROR_STOP=1 -At -c "
      UPDATE powersales.${TABLE} AS src
      SET ${SRC_FK} = tgt.${TARGET_PK}
      FROM powersales.${TARGET_TABLE} AS tgt
      WHERE src.${SRC_SFID} = tgt.sfid;
      SELECT 'updated';
    ")
    UPDATE_COUNT=$((UPDATE_COUNT + 1))
  done
done < <("$SCRIPT_DIR/lib/parse-list.sh")

echo ""
echo "[Phase 2] FK 매핑 처리 완료 ($UPDATE_COUNT 건)"

# ── constraint 복원 ──
RESTORE_SQL="${SF_MIGRATE_WORK_DIR}/restore.sql"
if [[ -f "$RESTORE_SQL" ]]; then
  "$SCRIPT_DIR/lib/constraints.sh" restore "$RESTORE_SQL"
else
  echo "WARN: restore.sql 이 없습니다 — Phase 1 을 단독 실행하지 않았는지 확인 (${RESTORE_SQL})"
fi

echo "[OK] Phase 2 완료"
