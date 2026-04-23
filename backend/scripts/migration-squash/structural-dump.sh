#!/usr/bin/env bash
# Flyway squash 검증용 구조 덤프 (정답 게이트)
#
# information_schema / pg_catalog 기반으로 테이블/컬럼/제약/인덱스/시퀀스/트리거를
# 정렬된 CSV 로 내보낸다. pg_dump 의 순서/포매팅 노이즈를 피해 "구조적 동일성"만 비교.
#
# 사용법:
#   structural-dump.sh <label>
#     <label>: 출력 디렉토리명 (예: A-dev, B-new)
#
# 환경변수: PGHOST, PGPORT, PGDATABASE, PGUSER (PGPASSWORD 권장: .pgpass)
#
# 출력 디렉토리 구조:
#   <label>/
#     01_tables.csv
#     02_columns.csv
#     03_constraints.csv
#     04_indexes.csv
#     05_sequences.csv
#     06_triggers.csv

set -euo pipefail

LABEL="${1:?label required}"
OUT_DIR="${OUT_DIR:-$(pwd)}/${LABEL}"
SCHEMA="${SCHEMA:-salesforce2}"

: "${PGHOST:?PGHOST required}"
: "${PGPORT:=5432}"
: "${PGDATABASE:?PGDATABASE required}"
: "${PGUSER:?PGUSER required}"

# psql 16 경로. 정보성 쿼리이므로 15도 동작하지만 pg_dump 와 버전 일관성을 위해 16 우선.
PG_BIN="${PG_BIN:-/opt/homebrew/opt/postgresql@16/bin}"
PSQL="${PG_BIN}/psql"
if [[ ! -x "$PSQL" ]]; then
  PSQL="psql"
fi

mkdir -p "$OUT_DIR"

psql_csv() {
  local sql="$1"
  local out="$2"
  "$PSQL" -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
    -v ON_ERROR_STOP=1 \
    --csv --no-align --tuples-only --quiet \
    -c "$sql" \
    | LC_ALL=C sort > "$out"
}

echo "[structural-dump] target: ${PGUSER}@${PGHOST}:${PGPORT}/${PGDATABASE} schema=${SCHEMA}"
echo "[structural-dump] out:    ${OUT_DIR}"

psql_csv "
  SELECT table_name
  FROM information_schema.tables
  WHERE table_schema = '${SCHEMA}'
    AND table_type = 'BASE TABLE'
    AND table_name NOT LIKE 'flyway_schema_history%';
" "${OUT_DIR}/01_tables.csv"

psql_csv "
  SELECT
    table_name,
    column_name,
    data_type,
    udt_name,
    is_nullable,
    COALESCE(column_default, ''),
    COALESCE(character_maximum_length::text, ''),
    COALESCE(numeric_precision::text, ''),
    COALESCE(numeric_scale::text, ''),
    COALESCE(datetime_precision::text, '')
  FROM information_schema.columns
  WHERE table_schema = '${SCHEMA}'
    AND table_name NOT LIKE 'flyway_schema_history%';
" "${OUT_DIR}/02_columns.csv"

psql_csv "
  SELECT
    c.conrelid::regclass::text AS table_name,
    c.conname,
    c.contype,
    pg_get_constraintdef(c.oid) AS definition
  FROM pg_constraint c
  JOIN pg_namespace n ON n.oid = c.connamespace
  WHERE n.nspname = '${SCHEMA}'
    AND c.conrelid::regclass::text NOT LIKE '%flyway_schema_history%';
" "${OUT_DIR}/03_constraints.csv"

psql_csv "
  SELECT tablename, indexname, indexdef
  FROM pg_indexes
  WHERE schemaname = '${SCHEMA}'
    AND tablename NOT LIKE 'flyway_schema_history%';
" "${OUT_DIR}/04_indexes.csv"

# 시퀀스는 이름/시작값/증가값/최소/최대/순환 여부만. 현재 last_value 는 데이터 기반이므로 제외.
psql_csv "
  SELECT
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment,
    cycle_option
  FROM information_schema.sequences
  WHERE sequence_schema = '${SCHEMA}';
" "${OUT_DIR}/05_sequences.csv"

psql_csv "
  SELECT
    c.relname AS table_name,
    t.tgname AS trigger_name,
    pg_get_triggerdef(t.oid) AS definition
  FROM pg_trigger t
  JOIN pg_class c ON c.oid = t.tgrelid
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE NOT t.tgisinternal
    AND n.nspname = '${SCHEMA}';
" "${OUT_DIR}/06_triggers.csv"

echo "[structural-dump] done:"
for f in "${OUT_DIR}"/*.csv; do
  echo "  $(basename "$f"): $(wc -l < "$f") rows"
done
