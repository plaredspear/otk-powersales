#!/usr/bin/env bash
#
# ============================================================================
#  SF Object 데이터 마이그레이션 진입점 (Spec #744, entity-driven)
# ============================================================================
#
# Phase 1 (적재) → Phase 2 (FK 연결) → verify 순차 실행.
# 입력은 SObject API name (예: Org__c). 도구가 backend/src/main/kotlin 에서
# @SFObject("...") 부착 entity 를 자동 탐색하여 메타 (@SFField/@Column/@JoinColumn)
# 를 추출하고 SOQL/매핑/FK 를 동적으로 구성한다.
#
# ── 사용법 ──────────────────────────────────────────────────────────────────
#
#   migrate.sh <SObject_API_Name>             # 단일 sobject 전체 단계
#   migrate.sh --all                          # 모든 @SFObject entity (의존성 순서 자동)
#   migrate.sh <sobject> --phase1-only        # 단일 sobject Phase 1 만
#   migrate.sh <sobject> --phase2-only        # 단일 sobject Phase 2 만
#   migrate.sh <sobject> --verify-only        # 단일 sobject verify 만
#   migrate.sh --all --phase1-only            # 전체 entity Phase 1 만
#
# ── 필수 환경변수 ───────────────────────────────────────────────────────────
#
#   SF_TARGET_ORG                  SF CLI 의 org alias 명
#                                  사전 등록:
#                                    sf org login web --alias <alias>
#                                  현재 등록 alias 확인:
#                                    sf org list
#                                  설정 예:
#                                    export SF_TARGET_ORG=otg-sandbox
#
#   DEV_OTK_PWRS_DB_PASSWORD       Dev DB (powersales schema) 비밀번호
#                                  사전 — SSM 터널 활성화 필요:
#                                    scripts/db-tunnel -p dev-otk-pwrs-db-access
#                                  비밀번호 조회:
#                                    scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password
#                                  설정 예:
#                                    export DEV_OTK_PWRS_DB_PASSWORD="$(scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password)"
#
# ── 선택 환경변수 (기본값 사용 권장) ────────────────────────────────────────
#
#   PGHOST                         기본: localhost (SSM 터널 종단)
#   PGPORT                         기본: 15432
#   PGUSER                         기본: otoki_admin
#   PGDATABASE                     기본: otoki
#   SF_MIGRATE_BULK_THRESHOLD      Bulk API 전환 row 임계 (기본: 5000)
#   SF_MIGRATE_KEEP_WORK           "true" 면 /tmp/sf-migrate/<ts> 보존 (기본: 정리)
#
# ── 사전 조건 ───────────────────────────────────────────────────────────────
#
#   - sf CLI v2.x 설치 + `sf org login web --alias <alias>` 완료
#   - SSM 터널 활성: `scripts/db-tunnel -p dev-otk-pwrs-db-access`
#   - python3 + jq + psql 설치
#
# ── 실행 예시 ───────────────────────────────────────────────────────────────
#
#   export SF_TARGET_ORG=otg-sandbox
#   export DEV_OTK_PWRS_DB_PASSWORD="$(scripts/db-tunnel.sh -p dev-otk-pwrs-db-access --password)"
#
#   backend/scripts/sf-migrate/migrate.sh Org__c
#   backend/scripts/sf-migrate/migrate.sh ProductBarcode__c
#   backend/scripts/sf-migrate/migrate.sh --all
#
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export SF_MIGRATE_DIR="$SCRIPT_DIR"
export SF_MIGRATE_META="$SCRIPT_DIR/lib/entity-meta.py"

# ── 옵션 파싱 ──
RUN_PHASE1=true
RUN_PHASE2=true
RUN_VERIFY=true
LIST_ALL=false
SOBJECTS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --phase1-only)  RUN_PHASE1=true;  RUN_PHASE2=false; RUN_VERIFY=false; shift ;;
    --phase2-only)  RUN_PHASE1=false; RUN_PHASE2=true;  RUN_VERIFY=false; shift ;;
    --verify-only)  RUN_PHASE1=false; RUN_PHASE2=false; RUN_VERIFY=true;  shift ;;
    --all)          LIST_ALL=true; shift ;;
    -h|--help)
      grep -E '^#( |$)' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    --*) echo "Unknown option: $1" >&2; exit 2 ;;
    *) SOBJECTS+=("$1"); shift ;;
  esac
done

# ── 사전 조건 검증 ──
for cmd in sf psql python3 jq; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: $cmd 가 설치되어 있지 않습니다." >&2
    exit 1
  fi
done
: "${SF_TARGET_ORG:?환경변수 SF_TARGET_ORG 가 설정되어 있지 않습니다.}"
: "${DEV_OTK_PWRS_DB_PASSWORD:?환경변수 DEV_OTK_PWRS_DB_PASSWORD 가 설정되어 있지 않습니다.}"

export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-15432}"
export PGUSER="${PGUSER:-otoki_admin}"
export PGDATABASE="${PGDATABASE:-otoki}"
export PGPASSWORD="$DEV_OTK_PWRS_DB_PASSWORD"
export PGOPTIONS="--search_path=powersales,public"

# ── SObject 목록 결정 ──
if $LIST_ALL; then
  if [[ ${#SOBJECTS[@]} -gt 0 ]]; then
    echo "ERROR: --all 과 SObject 명을 동시에 지정할 수 없습니다." >&2
    exit 2
  fi
  mapfile -t SOBJECTS < <(python3 "$SF_MIGRATE_META" --list-all --tables-only)
fi

if [[ ${#SOBJECTS[@]} -eq 0 ]]; then
  echo "ERROR: SObject API name 또는 --all 인자가 필요합니다." >&2
  echo "       사용법: $0 <SObject_API_Name>" >&2
  exit 2
fi

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
echo " sobjects (${#SOBJECTS[@]}): ${SOBJECTS[*]}"
echo "==============================================="

if $RUN_PHASE1; then
  "$SCRIPT_DIR/phase1-load.sh" "${SOBJECTS[@]}"
fi

if $RUN_PHASE2; then
  "$SCRIPT_DIR/phase2-link-fk.sh" "${SOBJECTS[@]}"
fi

if $RUN_VERIFY; then
  "$SCRIPT_DIR/verify.sh" "${SOBJECTS[@]}"
fi

echo "[OK] migrate.sh 완료"
