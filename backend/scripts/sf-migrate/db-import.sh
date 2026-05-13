#!/usr/bin/env bash
#
# ============================================================================
#  CSV → DB 적재 (SF org 미접근)
# ============================================================================
#
# sf-export.sh 가 생성한 CSV 를 Dev DB 에 적재한다. SF org 에 일체 접근하지
# 않으므로 SF_TARGET_ORG 환경변수도 불요.
#
# ── 동작 ────────────────────────────────────────────────────────────────────
#   1) entity-meta.py 로 table / FK 매핑 메타 추출
#   2) 대상 테이블의 FK constraint + NOT NULL constraint 백업 + 임시 DROP
#   3) TRUNCATE → \copy 적재 → post-load/<EntityClass>.sql 적용 (선택)
#   4) FK UPDATE: *_sfid → 참조 table.sfid → 로컬 PK 로 매핑
#      (target table 이 DB 에 비어 있으면 자동 skip)
#   5) constraint 복원
#   6) sfid NULL 검증 (= 0 이어야 통과)
#   7) text 컬럼 빈 문자열 검증 (정책 위반 알람 — 아래 NULL/빈 문자열 정책 참조)
#
# ── NULL / 빈 문자열 정책 ───────────────────────────────────────────────────
#   SF CSV 의 빈 셀은 모두 PG NULL 로 적재한다 (\copy FORMAT csv 기본 동작).
#   SF 측 NULL 과 빈 문자열은 CSV 단계에서 구분 불가능 — Apex 코드 조사 결과
#   (2026-05-14) 둘을 다른 분기로 라우팅하는 사례 없어 동등 통일이 안전.
#   적재 후 step 7 에서 text 컬럼 중 `''` 가 발견되면 WARN 출력 — 의외 케이스
#   (예: post-load SQL 이 빈 문자열을 명시 입력) 진단용. 정책 위반은 아니다.
#
# ── 사용법 ──────────────────────────────────────────────────────────────────
#   db-import.sh <SObject_API_Name> [csv_path]
#
#   기본 입력 경로: backend/scripts/sf-migrate/export/<SObject>.csv
#
# ── 필수 환경변수 ───────────────────────────────────────────────────────────
#   DEV_OTK_PWRS_DB_PASSWORD       Dev DB 비밀번호
#                                  사전 — SSM 터널 활성:
#                                    scripts/db-tunnel -p dev-otk-pwrs-db-access
#                                  조회:
#                                    scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password
#                                  설정 예:
#                                    export DEV_OTK_PWRS_DB_PASSWORD="$(scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password)"
#
# ── 선택 환경변수 (기본값 사용 권장) ────────────────────────────────────────
#   PGHOST                         기본: 127.0.0.1 (IPv4 강제 — ::1 회피)
#   PGPORT                         기본: 15432
#   PGUSER                         기본: otoki_admin
#   PGDATABASE                     기본: otoki  (※ powersales 는 schema 명)
#
# ── 실행 예시 ───────────────────────────────────────────────────────────────
#   export DEV_OTK_PWRS_DB_PASSWORD="$(scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password)"
#   backend/scripts/sf-migrate/db-import.sh Account
#
#   # CSV 검토 후 별도 경로
#   backend/scripts/sf-migrate/db-import.sh Account /tmp/account-2026-05.csv
#
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SOBJECT="${1:?SObject API name 필수}"
CSV_PATH="${2:-${SCRIPT_DIR}/export/${SOBJECT}.csv}"

# 사전 조건
for cmd in psql python3 jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: $cmd 가 설치되어 있지 않습니다." >&2
    exit 1
  fi
done
if [[ ! -f "$CSV_PATH" ]]; then
  echo "ERROR: CSV 파일이 없습니다: $CSV_PATH" >&2
  echo "       먼저 sf-export.sh $SOBJECT 를 실행하세요." >&2
  exit 1
fi
: "${DEV_OTK_PWRS_DB_PASSWORD:?환경변수 DEV_OTK_PWRS_DB_PASSWORD 가 설정되어 있지 않습니다.}"

export PGHOST="${PGHOST:-127.0.0.1}"
export PGPORT="${PGPORT:-15432}"
export PGUSER="${PGUSER:-otoki_admin}"
export PGDATABASE="${PGDATABASE:-otoki}"
export PGPASSWORD="$DEV_OTK_PWRS_DB_PASSWORD"
export PGOPTIONS="--search_path=powersales,public"

if [[ "$PGDATABASE" == "powersales" ]]; then
  echo "ERROR: PGDATABASE='powersales' 는 schema 명입니다. DB 명은 'otoki'." >&2
  echo "       unset PGDATABASE 또는 export PGDATABASE=otoki 후 재실행하세요." >&2
  exit 1
fi

# ── 1) 메타 추출 ──
META=$(python3 "$SCRIPT_DIR/lib/entity-meta.py" "$SOBJECT")
ENTITY_CLASS=$(echo "$META" | jq -r '.entity_class')
TABLE=$(echo "$META" | jq -r '.table')
SCHEMA=$(echo "$META" | jq -r '.schema')

echo "==============================================="
echo " DB Import  csv=$CSV_PATH  → ${SCHEMA}.${TABLE} ($ENTITY_CLASS)"
echo "==============================================="

# ── 2) constraint 백업 + DROP ──
RESTORE_SQL=$(mktemp -t sf-migrate-restore.XXXXXX.sql)
trap "rm -f $RESTORE_SQL" EXIT

# 2-1) FK constraint 복원 SQL
psql -At -c "
  SELECT format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s;',
                n.nspname, c.relname, con.conname, pg_get_constraintdef(con.oid))
  FROM pg_constraint con
  JOIN pg_class c ON c.oid = con.conrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE con.contype = 'f' AND n.nspname = '${SCHEMA}' AND c.relname = '${TABLE}'
  ORDER BY con.conname;
" > "$RESTORE_SQL"

# 2-2) NOT NULL 복원 SQL (default 가 없는 NOT NULL 컬럼만)
psql -At -c "
  SELECT format('ALTER TABLE %I.%I ALTER COLUMN %I SET NOT NULL;',
                table_schema, table_name, column_name)
  FROM information_schema.columns
  WHERE table_schema = '${SCHEMA}' AND table_name = '${TABLE}'
    AND is_nullable = 'NO' AND column_default IS NULL
  ORDER BY ordinal_position;
" >> "$RESTORE_SQL"

FK_COUNT=$(grep -c 'ADD CONSTRAINT' "$RESTORE_SQL" || true)
NN_COUNT=$(grep -c 'SET NOT NULL' "$RESTORE_SQL" || true)
echo "[db-import:$ENTITY_CLASS] constraint backup — FK=$FK_COUNT, NN=$NN_COUNT"

# 2-3) DROP
psql -At -c "
  SELECT format('ALTER TABLE %I.%I DROP CONSTRAINT %I;', n.nspname, c.relname, con.conname)
  FROM pg_constraint con
  JOIN pg_class c ON c.oid = con.conrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE con.contype = 'f' AND n.nspname = '${SCHEMA}' AND c.relname = '${TABLE}';
" | psql -v ON_ERROR_STOP=1 -q

psql -At -c "
  SELECT format('ALTER TABLE %I.%I ALTER COLUMN %I DROP NOT NULL;',
                table_schema, table_name, column_name)
  FROM information_schema.columns
  WHERE table_schema = '${SCHEMA}' AND table_name = '${TABLE}'
    AND is_nullable = 'NO' AND column_default IS NULL;
" | psql -v ON_ERROR_STOP=1 -q

# ── 3) TRUNCATE → COPY → post-load ──
DB_COLS=$(head -n 1 "$CSV_PATH")
if [[ -z "$DB_COLS" ]]; then
  echo "ERROR: CSV 헤더가 비어 있습니다." >&2
  exit 1
fi

psql -v ON_ERROR_STOP=1 -q -c "TRUNCATE TABLE ${SCHEMA}.${TABLE} RESTART IDENTITY CASCADE;"
psql -v ON_ERROR_STOP=1 -q -c "\copy ${SCHEMA}.${TABLE}(${DB_COLS}) FROM '${CSV_PATH}' WITH (FORMAT csv, HEADER true)"

POST_SQL="${SCRIPT_DIR}/post-load/${ENTITY_CLASS}.sql"
if [[ -f "$POST_SQL" ]]; then
  psql -v ON_ERROR_STOP=1 -q -f "$POST_SQL"
  echo "[db-import:$ENTITY_CLASS] post-load 적용 ($(basename "$POST_SQL"))"
fi

LOADED=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TABLE};")
echo "[db-import:$ENTITY_CLASS] loaded=$LOADED"

# ── 4) FK UPDATE (target 이 DB 에 데이터를 보유한 경우만) ──
UPDATE_COUNT=0
SKIP_COUNT=0
while IFS=$'\t' read -r SRC_SFID SRC_FK TARGET_TABLE; do
  [[ -z "$SRC_SFID" ]] && continue

  TARGET_ROWS=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TARGET_TABLE};" 2>/dev/null || echo 0)
  if [[ "$TARGET_ROWS" -eq 0 ]]; then
    echo "[db-import:$ENTITY_CLASS] skip FK ${SRC_FK} ← ${TARGET_TABLE} (target 비어 있음)"
    SKIP_COUNT=$((SKIP_COUNT + 1))
    continue
  fi

  TARGET_PK=$(psql -At -c "
    SELECT a.attname
    FROM pg_index i
    JOIN pg_class c ON c.oid = i.indrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
    WHERE i.indisprimary AND n.nspname = '${SCHEMA}' AND c.relname = '${TARGET_TABLE}';
  ")
  if [[ -z "$TARGET_PK" ]]; then
    echo "WARN: target ${TARGET_TABLE} 의 PK 미확인 — skip" >&2
    SKIP_COUNT=$((SKIP_COUNT + 1))
    continue
  fi

  echo "[db-import:$ENTITY_CLASS] FK ${TABLE}.${SRC_FK} ← ${TARGET_TABLE}.${TARGET_PK} (via ${SRC_SFID})"
  psql -v ON_ERROR_STOP=1 -q -c "
    UPDATE ${SCHEMA}.${TABLE} AS src
    SET ${SRC_FK} = tgt.${TARGET_PK}
    FROM ${SCHEMA}.${TARGET_TABLE} AS tgt
    WHERE src.${SRC_SFID} = tgt.sfid;
  "
  UPDATE_COUNT=$((UPDATE_COUNT + 1))
done < <(echo "$META" | jq -r '.fk_mappings[] | [.src_sfid, .src_fk, .target_table] | @tsv')

echo "[db-import:$ENTITY_CLASS] FK UPDATE: $UPDATE_COUNT 건 실행, $SKIP_COUNT 건 skip"

# ── 5) constraint 복원 ──
psql -v ON_ERROR_STOP=1 -q -f "$RESTORE_SQL"
echo "[db-import:$ENTITY_CLASS] constraint 복원 완료"

# ── 6) sfid NULL 검증 ──
SFID_NULL=$(psql -At -c "SELECT COUNT(*) FROM ${SCHEMA}.${TABLE} WHERE sfid IS NULL;")
echo "[db-import:$ENTITY_CLASS] sfid_null=$SFID_NULL"
if [[ "$SFID_NULL" -ne 0 ]]; then
  echo "[FAIL] sfid NULL > 0" >&2
  exit 1
fi

# ── 7) 빈 문자열 검증 (NULL 통일 정책 — 발견 시 WARN, FAIL 아님) ──
psql -v ON_ERROR_STOP=1 -q <<SQL
DO \$\$
DECLARE
  rec record;
  cnt bigint;
BEGIN
  FOR rec IN
    SELECT column_name FROM information_schema.columns
    WHERE table_schema = '${SCHEMA}' AND table_name = '${TABLE}'
      AND data_type IN ('character varying', 'text', 'character')
  LOOP
    EXECUTE format('SELECT COUNT(*) FROM ${SCHEMA}.${TABLE} WHERE %I = ''''', rec.column_name) INTO cnt;
    IF cnt > 0 THEN
      RAISE WARNING '[db-import:${ENTITY_CLASS}] empty string in column %: % rows (NULL 통일 정책 위반 — 의외 케이스 점검)', rec.column_name, cnt;
    END IF;
  END LOOP;
END \$\$;
SQL

echo "[OK] db-import 완료 — ${SCHEMA}.${TABLE} rows=$LOADED"
