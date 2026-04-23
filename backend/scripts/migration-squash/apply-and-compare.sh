#!/usr/bin/env bash
# Flyway squash 검증 원샷 스크립트
#
# 사용자가 새 V1__init_schema.sql 을 작성한 뒤 반복적으로 호출한다.
# Docker 로 빈 postgres:16 을 띄우고, 새 V1 을 적용한 결과(B) 를
# 사전에 떠놓은 dev-db 스냅샷(A) 과 구조적으로 비교한다.
#
# 사용법:
#   apply-and-compare.sh [V1-path] [B-label]
#     V1-path: 기본 ../../src/main/resources/db/migration/V1__init_schema.sql
#     B-label: 기본 B-new
#
# 전제:
#   - Docker 데몬 실행 중
#   - 같은 디렉토리 하위에 snapshots/A-dev, snapshots/A-dev.normalized.sql 이 이미 존재
#
# Exit code: compare.sh 결과 그대로 (0 통과, 1 diff).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SNAP_DIR="${SCRIPT_DIR}/snapshots"

V1_PATH="${1:-${SCRIPT_DIR}/../../src/main/resources/db/migration/V1__init_schema.sql}"
LABEL="${2:-B-new}"
CONTAINER="pg-squash-verify"
PORT="55432"

if [[ ! -f "$V1_PATH" ]]; then
  echo "ERROR: V1 file not found: $V1_PATH" >&2
  exit 2
fi

if ! command -v docker >/dev/null; then
  echo "ERROR: docker not installed" >&2
  exit 2
fi

if ! docker info >/dev/null 2>&1; then
  echo "ERROR: docker daemon not running" >&2
  exit 2
fi

if [[ ! -d "${SNAP_DIR}/A-dev" ]]; then
  echo "ERROR: baseline snapshot missing: ${SNAP_DIR}/A-dev" >&2
  echo "       Run Phase 1 (dump-schema.sh + structural-dump.sh against dev-db) first." >&2
  exit 2
fi

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[apply-and-compare] V1:    $V1_PATH"
echo "[apply-and-compare] label: $LABEL"
echo

# 1. 빈 postgres:16 기동
cleanup
docker run -d \
  --name "$CONTAINER" \
  -e POSTGRES_HOST_AUTH_METHOD=trust \
  -p "${PORT}:5432" \
  postgres:16 >/dev/null

echo "[apply-and-compare] waiting for postgres..."
for i in {1..30}; do
  if docker exec "$CONTAINER" pg_isready -U postgres >/dev/null 2>&1; then
    break
  fi
  sleep 1
  if [[ $i == 30 ]]; then
    echo "ERROR: postgres did not become ready in 30s" >&2
    exit 2
  fi
done

# 2. DB/스키마 준비 (기존 V1 이 기대하는 환경)
docker exec -i "$CONTAINER" psql -U postgres -v ON_ERROR_STOP=1 <<'SQL' >/dev/null
CREATE DATABASE otoki;
SQL

docker exec -i "$CONTAINER" psql -U postgres -d otoki -v ON_ERROR_STOP=1 <<'SQL' >/dev/null
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE SCHEMA IF NOT EXISTS salesforce2;
SET search_path TO salesforce2;
SQL

# 3. 새 V1 적용 — V1 파일 안의 모든 DDL 은 이미 schema-qualified (salesforce2.xxx) 이므로
#    search_path 설정 불필요. -c 와 stdin 은 상호 배타적이라 -c 를 쓰면 stdin 이 무시됨.
echo "[apply-and-compare] applying V1..."
if ! docker exec -i "$CONTAINER" psql -U postgres -d otoki -v ON_ERROR_STOP=1 \
      < "$V1_PATH" >/tmp/apply-v1.log 2>&1; then
  echo "ERROR: V1 apply failed. Log:" >&2
  tail -60 /tmp/apply-v1.log >&2
  exit 2
fi
echo "[apply-and-compare] V1 applied OK ($(wc -l < /tmp/apply-v1.log) log lines)."

# 4. B 스냅샷 덤프
(
  cd "$SNAP_DIR"
  PGHOST=localhost PGPORT="$PORT" PGDATABASE=otoki PGUSER=postgres \
    "${SCRIPT_DIR}/dump-schema.sh" "$LABEL"
  PGHOST=localhost PGPORT="$PORT" PGDATABASE=otoki PGUSER=postgres \
    "${SCRIPT_DIR}/structural-dump.sh" "$LABEL"
)

# 5. 비교
echo
echo "[apply-and-compare] comparing A-dev vs ${LABEL}..."
(cd "$SNAP_DIR" && "${SCRIPT_DIR}/compare.sh" A-dev "$LABEL")
