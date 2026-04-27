#!/usr/bin/env bash
#
# db-reset.sh
#
# 타겟 DB 의 powersales 스키마 전체 데이터 초기화 (전체 테이블 일괄, 부분 선택 불가).
#
# 두 가지 모드:
#   truncate (기본): powersales 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE.
#                    flyway_schema_history 는 보존 → backend Flyway 가 마이그레이션을 재실행하지 않음.
#   recreate       : DROP SCHEMA powersales CASCADE; CREATE SCHEMA powersales (owner 보존).
#                    flyway_schema_history 도 함께 사라짐 → backend Flyway 가 모든 마이그레이션을 재실행함.
#
# 사전 조건:
#   - 별도 터미널에서 scripts/db-tunnel.sh -s <stage> 가 떠 있어야 함.
#   - 비밀번호 환경변수: dev → DEV_OTK_PWRS_DB_PASSWORD, prod → PROD_OTK_PWRS_DB_PASSWORD
#
# Usage:
#   scripts/db-reset.sh                          # dev, truncate
#   scripts/db-reset.sh --mode recreate          # dev, recreate
#   scripts/db-reset.sh -s prod                  # prod, truncate (추가 확인 프롬프트)
#   scripts/db-reset.sh -s prod --mode recreate  # prod, recreate
#
set -euo pipefail

TARGET_SCHEMA="powersales"

################################################################################
# 유틸리티
################################################################################

log()  { echo "$(date '+%H:%M:%S') [INFO]  $*"; }
warn() { echo "$(date '+%H:%M:%S') [WARN]  $*" >&2; }
err()  { echo "$(date '+%H:%M:%S') [ERROR] $*" >&2; }

confirm() {
  local msg="$1"
  read -r -p "$msg [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]]
}

################################################################################
# 인자 파싱
################################################################################

STAGE="dev"
MODE="truncate"

while [[ $# -gt 0 ]]; do
  case $1 in
    -s|--stage) STAGE="$2"; shift 2 ;;
    --mode)     MODE="$2"; shift 2 ;;
    -h|--help)
      cat <<EOH
Usage: $0 [options]

Options:
  -s, --stage <name>  타겟 DB stage (dev | prod) — 기본 dev
  --mode <mode>       reset 모드 (truncate | recreate) — 기본 truncate
  -h, --help          사용법

모드:
  truncate  powersales 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE.
            flyway_schema_history 는 보존 (backend 가 마이그레이션을 다시 실행하지 않음).

  recreate  DROP SCHEMA powersales CASCADE; CREATE SCHEMA powersales (owner 보존).
            flyway_schema_history 도 사라짐 → backend Flyway 가 모든 마이그레이션을 재실행해야 함.
EOH
      exit 0
      ;;
    *) err "Unknown option: $1"; exit 1 ;;
  esac
done

################################################################################
# stage / mode 검증
################################################################################

case "$MODE" in
  truncate|recreate) ;;
  *) err "지원하지 않는 mode: $MODE (truncate, recreate)"; exit 1 ;;
esac

case "$STAGE" in
  dev)
    TARGET_HOST="localhost"; TARGET_PORT="15432"
    TARGET_USER="otkadmin"; TARGET_DB="otoki"
    TARGET_PASS="${DEV_OTK_PWRS_DB_PASSWORD:-}"
    PASS_VAR="DEV_OTK_PWRS_DB_PASSWORD"
    ;;
  prod)
    TARGET_HOST="localhost"; TARGET_PORT="25432"
    TARGET_USER="postgres"; TARGET_DB="otoki"
    TARGET_PASS="${PROD_OTK_PWRS_DB_PASSWORD:-}"
    PASS_VAR="PROD_OTK_PWRS_DB_PASSWORD"
    ;;
  *) err "지원하지 않는 stage: $STAGE (dev, prod)"; exit 1 ;;
esac

if [[ -z "$TARGET_PASS" ]]; then
  err "환경변수 \$$PASS_VAR 가 설정되지 않았습니다."
  err "  scripts/db-tunnel.sh -s $STAGE --password 로 조회 후 export 하세요."
  exit 1
fi

################################################################################
# 사전 검증
################################################################################

if ! command -v psql &>/dev/null; then
  err "psql 이 설치되어 있지 않습니다."; exit 1
fi

if ! nc -z "$TARGET_HOST" "$TARGET_PORT" &>/dev/null; then
  err "타겟 포트가 열려있지 않습니다: $TARGET_HOST:$TARGET_PORT"
  err "  별도 터미널에서 다음을 먼저 실행하세요: scripts/db-tunnel.sh -s $STAGE"
  exit 1
fi

PSQL=(env PGPASSWORD="$TARGET_PASS" psql -h "$TARGET_HOST" -p "$TARGET_PORT" -U "$TARGET_USER" -d "$TARGET_DB")

if ! "${PSQL[@]}" -c "SELECT 1;" &>/dev/null; then
  err "타겟 DB 접속 실패: $TARGET_HOST:$TARGET_PORT/$TARGET_DB (user=$TARGET_USER)"
  exit 1
fi

log "타겟: $TARGET_HOST:$TARGET_PORT/$TARGET_DB (stage=$STAGE) 스키마=$TARGET_SCHEMA"
log "모드: $MODE"

################################################################################
# 사전 정보 출력 (영향 받는 테이블 / 현재 행 수)
################################################################################

log "$TARGET_SCHEMA 의 현재 테이블 행 수 (상위):"
"${PSQL[@]}" -c "
SELECT n.nspname || '.' || c.relname AS table_name,
       (xpath('/row/cnt/text()', xml_count))[1]::text::bigint AS row_count
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
CROSS JOIN LATERAL (
  SELECT query_to_xml(format('SELECT count(*) AS cnt FROM %I.%I', n.nspname, c.relname), false, true, '') AS xml_count
) x
WHERE c.relkind = 'r' AND n.nspname = '$TARGET_SCHEMA'
ORDER BY 2 DESC, 1;
"

################################################################################
# 사용자 확인
################################################################################

if [[ "$STAGE" == "prod" ]]; then
  warn "⚠️  운영(prod) DB 의 $TARGET_SCHEMA 스키마를 초기화합니다!"
fi

if [[ "$MODE" == "recreate" ]]; then
  warn "RECREATE 모드: flyway_schema_history 도 함께 삭제됩니다."
  warn "이후 backend 가 모든 Flyway 마이그레이션을 재실행해야 합니다."
fi

if ! confirm "정말 $TARGET_SCHEMA 스키마를 초기화하시겠습니까?"; then
  log "사용자에 의해 취소되었습니다."
  exit 0
fi

################################################################################
# 실행
################################################################################

if [[ "$MODE" == "truncate" ]]; then
  log "=== TRUNCATE 모드 실행 ==="
  log "$TARGET_SCHEMA 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE (flyway_schema_history 제외)"

  "${PSQL[@]}" <<EOSQL
DO \$do\$
DECLARE
  table_list text;
BEGIN
  SELECT string_agg(quote_ident(table_schema) || '.' || quote_ident(table_name), ', ')
    INTO table_list
  FROM information_schema.tables
  WHERE table_schema = '$TARGET_SCHEMA'
    AND table_type = 'BASE TABLE'
    AND table_name <> 'flyway_schema_history';

  IF table_list IS NULL THEN
    RAISE NOTICE '$TARGET_SCHEMA 에 대상 테이블이 없습니다.';
  ELSE
    RAISE NOTICE '대상 테이블 목록: %', table_list;
    EXECUTE 'TRUNCATE TABLE ' || table_list || ' RESTART IDENTITY CASCADE';
    RAISE NOTICE 'TRUNCATE 완료';
  END IF;
END
\$do\$;
EOSQL

elif [[ "$MODE" == "recreate" ]]; then
  log "=== RECREATE 모드 실행 ==="

  OWNER=$("${PSQL[@]}" -t -A -c "SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname='$TARGET_SCHEMA';")
  if [[ -z "$OWNER" ]]; then
    err "$TARGET_SCHEMA 스키마를 찾을 수 없습니다."; exit 1
  fi
  log "기존 스키마 owner: $OWNER"

  log "DROP SCHEMA $TARGET_SCHEMA CASCADE; CREATE SCHEMA $TARGET_SCHEMA AUTHORIZATION $OWNER;"
  "${PSQL[@]}" <<EOSQL
DROP SCHEMA $TARGET_SCHEMA CASCADE;
CREATE SCHEMA $TARGET_SCHEMA AUTHORIZATION $OWNER;
EOSQL

  log "완료. backend Flyway 가 모든 마이그레이션을 다시 실행해야 합니다:"
  log "  cd backend && ./gradlew flywayMigrate    # 또는 ./gradlew bootRun"
fi

log "=== 완료 ==="
