#!/usr/bin/env bash
# Flyway squash 검증용 스키마 덤프 스크립트
#
# 사용법:
#   dump-schema.sh <label>
#     <label>: 출력 파일 접두어 (예: A-dev, B-new)
#
# 접속 정보는 환경변수 또는 ~/.pgpass 사용:
#   PGHOST, PGPORT, PGDATABASE, PGUSER  (PGPASSWORD 는 .pgpass 권장)
#
# 출력:
#   <label>.raw.sql         - pg_dump 원본
#   <label>.normalized.sql  - 노이즈 제거본 (텍스트 diff 용)
#
# 비교 대상: salesforce2 스키마, flyway_schema_history 제외.

set -euo pipefail

LABEL="${1:?label required (e.g., A-dev, B-new)}"
OUT_DIR="${OUT_DIR:-$(pwd)}"

: "${PGHOST:?PGHOST required}"
: "${PGPORT:=5432}"
: "${PGDATABASE:?PGDATABASE required}"
: "${PGUSER:?PGUSER required}"

# pg_dump 16 경로 (서버가 16.x 이므로 필수). 비어 있으면 PATH 사용.
PG_BIN="${PG_BIN:-/opt/homebrew/opt/postgresql@16/bin}"
PG_DUMP="${PG_BIN}/pg_dump"
if [[ ! -x "$PG_DUMP" ]]; then
  PG_DUMP="pg_dump"  # fallback to PATH
fi

RAW="${OUT_DIR}/${LABEL}.raw.sql"
NORM="${OUT_DIR}/${LABEL}.normalized.sql"

echo "[dump-schema] ${PGUSER}@${PGHOST}:${PGPORT}/${PGDATABASE} -> ${RAW}"

"$PG_DUMP" \
  --schema-only \
  --no-owner \
  --no-privileges \
  --no-comments \
  --schema=salesforce2 \
  --exclude-table='salesforce2.flyway_schema_history' \
  --exclude-table='salesforce2.flyway_schema_history_backup_*' \
  -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
  > "$RAW"

# 정규화: SET/SELECT pg_catalog 세팅 제거, \restrict 토큰(호출마다 랜덤) 제거,
# 주석 제거, 연속 공백 정리
awk '
  /^SET / { next }
  /^SELECT pg_catalog\./ { next }
  /^\\restrict / { next }
  /^\\unrestrict / { next }
  /^--/ { next }
  /^$/ { if (blank++) next; print; next }
  { blank=0; print }
' "$RAW" > "$NORM"

echo "[dump-schema] done."
echo "  raw:        $(wc -l < "$RAW") lines, $(wc -c < "$RAW") bytes"
echo "  normalized: $(wc -l < "$NORM") lines, $(wc -c < "$NORM") bytes"
