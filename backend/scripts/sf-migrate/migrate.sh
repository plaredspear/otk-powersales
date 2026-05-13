#!/usr/bin/env bash
#
# SF Object 데이터 마이그레이션 진입점 (Spec #744)
#
# Phase 1 (적재) → Phase 2 (FK 연결) → verify 순차 실행.
#
# 사전 조건:
#   - sf CLI 설치 + `sf org login web` 완료, 환경변수 SF_TARGET_ORG 설정
#   - scripts/db-tunnel 으로 localhost:15432 터널 활성
#   - 환경변수 DEV_OTK_PWRS_DB_PASSWORD 설정
#
# 사용법:
#   migrate.sh                      # Phase1 + Phase2 + verify 전체
#   migrate.sh --phase1-only
#   migrate.sh --phase2-only
#   migrate.sh --verify-only
#   migrate.sh --only entity1,entity2
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export SF_MIGRATE_DIR="$SCRIPT_DIR"

# ── 옵션 파싱 ──
RUN_PHASE1=true
RUN_PHASE2=true
RUN_VERIFY=true
ONLY_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --phase1-only)  RUN_PHASE1=true;  RUN_PHASE2=false; RUN_VERIFY=false; shift ;;
    --phase2-only)  RUN_PHASE1=false; RUN_PHASE2=true;  RUN_VERIFY=false; shift ;;
    --verify-only)  RUN_PHASE1=false; RUN_PHASE2=false; RUN_VERIFY=true;  shift ;;
    --only)         ONLY_FILTER="$2"; shift 2 ;;
    -h|--help)
      grep -E '^#( |$)' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

export SF_MIGRATE_ONLY="$ONLY_FILTER"

# ── 사전 조건 검증 ──
if ! command -v sf >/dev/null 2>&1; then
  echo "ERROR: sf CLI 가 설치되어 있지 않습니다." >&2
  exit 1
fi
if ! command -v psql >/dev/null 2>&1; then
  echo "ERROR: psql 이 설치되어 있지 않습니다." >&2
  exit 1
fi
: "${SF_TARGET_ORG:?환경변수 SF_TARGET_ORG 가 설정되어 있지 않습니다.}"
: "${DEV_OTK_PWRS_DB_PASSWORD:?환경변수 DEV_OTK_PWRS_DB_PASSWORD 가 설정되어 있지 않습니다.}"

export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-15432}"
export PGUSER="${PGUSER:-otoki_admin}"
export PGDATABASE="${PGDATABASE:-otoki}"
export PGPASSWORD="$DEV_OTK_PWRS_DB_PASSWORD"
export PGOPTIONS="--search_path=powersales,public"

# ── 작업 디렉토리 ──
TS="$(date +%Y%m%d-%H%M%S)"
WORK_DIR="/tmp/sf-migrate/$TS"
mkdir -p "$WORK_DIR"
export SF_MIGRATE_WORK_DIR="$WORK_DIR"

cleanup() {
  if [[ "${SF_MIGRATE_KEEP_WORK:-false}" != "true" ]]; then
    rm -rf "$WORK_DIR"
  fi
}
trap cleanup EXIT

echo "==============================================="
echo " SF Migration  worker=$WORK_DIR  org=$SF_TARGET_ORG"
echo " filter=${ONLY_FILTER:-(all)}"
echo "==============================================="

if $RUN_PHASE1; then
  "$SCRIPT_DIR/phase1-load.sh"
fi

if $RUN_PHASE2; then
  "$SCRIPT_DIR/phase2-link-fk.sh"
fi

if $RUN_VERIFY; then
  "$SCRIPT_DIR/verify.sh"
fi

echo "[OK] migrate.sh 완료"
