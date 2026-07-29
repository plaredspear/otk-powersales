#!/usr/bin/env bash
#
# db-reset.sh
#
# 타겟 DB 의 powersales 스키마 전체 데이터 초기화 (앱 버전 테이블 제외, 그 외 부분 선택 불가).
#
# 두 가지 모드:
#   truncate (기본): powersales 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE.
#                    flyway_schema_history 는 보존 → backend Flyway 가 마이그레이션을 재실행하지 않음.
#                    app_package (앱 버전 관리) 도 보존 → 초기화 후에도 모바일 배포 패키지가 유지됨.
#   recreate       : DROP SCHEMA powersales CASCADE; CREATE SCHEMA powersales (owner 보존).
#                    flyway_schema_history 도 함께 사라짐 → backend Flyway 가 모든 마이그레이션을 재실행함.
#                    app_package 도 테이블째 사라지므로 보존되지 않음 (truncate 를 쓸 것).
#
# 접속 정보 우선순위:
#   1) --db-properties <path>  로 명시한 properties 파일 (host/port/database/user/password)
#   2) -s | --stage <stage>    의 stage 분기 (dev/prod). 비밀번호는 환경변수
#                              (DEV_OTK_PWRS_DB_PASSWORD / PROD_OTK_PWRS_DB_PASSWORD).
#
# 사전 조건:
#   - 별도 터미널에서 scripts/db-tunnel.sh -s <stage> 가 떠 있어야 함.
#
# Usage:
#   scripts/db-reset.sh                                         # dev, truncate
#   scripts/db-reset.sh --mode recreate                         # dev, recreate
#   scripts/db-reset.sh -s prod                                 # prod, truncate (추가 확인 프롬프트)
#   scripts/db-reset.sh --db-properties scripts/sf-data-migration/db.properties
#   scripts/db-reset.sh --db-properties <path> --yes            # 사용자 확인 프롬프트 생략
#
set -euo pipefail

TARGET_SCHEMA="powersales"

# truncate 모드에서 데이터를 보존할 테이블 (앱 버전 관리 — SF 마이그레이션과 무관한 자체 엔티티).
# 옵션으로 노출하지 않는 고정 정책.
KEEP_TABLE="app_package"

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

# properties 파일에서 key=value 한 줄 추출 (주석 / 빈 줄 무시).
read_prop() {
  local file="$1" key="$2"
  awk -F'=' -v k="$key" '
    /^[[:space:]]*#/ { next }
    /^[[:space:]]*$/ { next }
    {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $1)
      if ($1 == k) {
        # value 는 첫 = 뒤 전체 (값에 = 포함 가능)
        sub(/^[^=]*=/, "", $0)
        print $0
        exit
      }
    }
  ' "$file"
}

################################################################################
# 인자 파싱
################################################################################

STAGE="dev"
MODE="truncate"
DB_PROPS=""
ASSUME_YES=0

while [[ $# -gt 0 ]]; do
  case $1 in
    -s|--stage)        STAGE="$2"; shift 2 ;;
    --mode)            MODE="$2"; shift 2 ;;
    --db-properties)   DB_PROPS="$2"; shift 2 ;;
    -y|--yes)          ASSUME_YES=1; shift ;;
    -h|--help)
      cat <<EOH
Usage: $0 [options]

Options:
  -s, --stage <name>           타겟 DB stage (dev | prod) — 기본 dev
  --mode <mode>                reset 모드 (truncate | recreate) — 기본 truncate
  --db-properties <path>       db.properties 파일 경로. 명시 시 stage 분기 무시하고
                               파일의 host/port/database/user/password 사용.
  -y, --yes                    사용자 확인 프롬프트 생략 (자동화용)
  -h, --help                   사용법

모드:
  truncate  powersales 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE.
            flyway_schema_history 는 보존 (backend 가 마이그레이션을 다시 실행하지 않음).
            $KEEP_TABLE (앱 버전 관리) 은 임시 보관 후 복원되어 데이터가 유지됨.

  recreate  DROP SCHEMA powersales CASCADE; CREATE SCHEMA powersales (owner 보존).
            flyway_schema_history 도 사라짐 → backend Flyway 가 모든 마이그레이션을 재실행해야 함.
            $KEEP_TABLE 도 함께 사라짐 (보존 불가).
EOH
      exit 0
      ;;
    *) err "Unknown option: $1"; exit 1 ;;
  esac
done

################################################################################
# mode 검증
################################################################################

case "$MODE" in
  truncate|recreate) ;;
  *) err "지원하지 않는 mode: $MODE (truncate, recreate)"; exit 1 ;;
esac

################################################################################
# 접속 정보 결정 — db.properties 우선, 없으면 stage 분기
################################################################################

if [[ -n "$DB_PROPS" ]]; then
  if [[ ! -f "$DB_PROPS" ]]; then
    err "db.properties 를 찾을 수 없습니다: $DB_PROPS"
    exit 1
  fi
  TARGET_HOST="$(read_prop "$DB_PROPS" host)"
  TARGET_PORT="$(read_prop "$DB_PROPS" port)"
  TARGET_DB="$(read_prop   "$DB_PROPS" database)"
  TARGET_USER="$(read_prop "$DB_PROPS" user)"
  TARGET_PASS="$(read_prop "$DB_PROPS" password)"
  PROPS_SCHEMA="$(read_prop "$DB_PROPS" schema)"
  if [[ -n "$PROPS_SCHEMA" ]]; then
    TARGET_SCHEMA="$PROPS_SCHEMA"
  fi
  SOURCE_LABEL="db.properties=$DB_PROPS"

  if [[ -z "$TARGET_HOST" || -z "$TARGET_PORT" || -z "$TARGET_DB" || -z "$TARGET_USER" || -z "$TARGET_PASS" ]]; then
    err "db.properties 에 필수 키 누락 (host/port/database/user/password): $DB_PROPS"
    exit 1
  fi
else
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
  SOURCE_LABEL="stage=$STAGE"
fi

################################################################################
# 사전 검증
################################################################################

if ! command -v psql &>/dev/null; then
  err "psql 이 설치되어 있지 않습니다."; exit 1
fi

if ! nc -z "$TARGET_HOST" "$TARGET_PORT" &>/dev/null; then
  err "타겟 포트가 열려있지 않습니다: $TARGET_HOST:$TARGET_PORT"
  err "  별도 터미널에서 다음을 먼저 실행하세요: scripts/db-tunnel.sh -s <stage>"
  exit 1
fi

PSQL=(env PGPASSWORD="$TARGET_PASS" psql -h "$TARGET_HOST" -p "$TARGET_PORT" -U "$TARGET_USER" -d "$TARGET_DB")

if ! "${PSQL[@]}" -c "SELECT 1;" &>/dev/null; then
  err "타겟 DB 접속 실패: $TARGET_HOST:$TARGET_PORT/$TARGET_DB (user=$TARGET_USER)"
  exit 1
fi

log "타겟: $TARGET_HOST:$TARGET_PORT/$TARGET_DB ($SOURCE_LABEL) 스키마=$TARGET_SCHEMA"
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

if [[ "${STAGE:-}" == "prod" && -z "$DB_PROPS" ]]; then
  warn "⚠️  운영(prod) DB 의 $TARGET_SCHEMA 스키마를 초기화합니다!"
fi

if [[ "$MODE" == "recreate" ]]; then
  warn "RECREATE 모드: flyway_schema_history 도 함께 삭제됩니다."
  warn "이후 backend 가 모든 Flyway 마이그레이션을 재실행해야 합니다."
  warn "RECREATE 모드에서는 $KEEP_TABLE (앱 버전 관리) 이 보존되지 않습니다 — 보존하려면 truncate 모드를 쓰세요."
else
  log "보존 대상: flyway_schema_history, $TARGET_SCHEMA.$KEEP_TABLE (앱 버전 관리)"
fi

if [[ $ASSUME_YES -eq 0 ]]; then
  if ! confirm "정말 $TARGET_SCHEMA 스키마를 초기화하시겠습니까?"; then
    log "사용자에 의해 취소되었습니다."
    exit 0
  fi
fi

################################################################################
# 실행
################################################################################

if [[ "$MODE" == "truncate" ]]; then
  log "=== TRUNCATE 모드 실행 ==="
  log "$TARGET_SCHEMA 의 모든 테이블 TRUNCATE RESTART IDENTITY CASCADE (flyway_schema_history, $KEEP_TABLE 제외)"

  # $KEEP_TABLE 은 TRUNCATE 목록에서 빼는 것만으로는 보존되지 않는다.
  # employee 를 FK 로 참조하므로 CASCADE 가 함께 비우기 때문 (TRUNCATE 는 ON DELETE 규칙을 따르지 않음).
  # → 같은 트랜잭션 안에서 임시 테이블에 보관했다가 TRUNCATE 후 되돌린다.
  KEEP_EXISTS="$("${PSQL[@]}" -t -A -c "SELECT to_regclass('$TARGET_SCHEMA.$KEEP_TABLE') IS NOT NULL;")"

  KEEP_PRE_SQL=""
  KEEP_POST_SQL=""
  if [[ "$KEEP_EXISTS" == "t" ]]; then
    KEEP_ROWS="$("${PSQL[@]}" -t -A -c "SELECT count(*) FROM $TARGET_SCHEMA.$KEEP_TABLE;")"
    log "보존: $TARGET_SCHEMA.$KEEP_TABLE ($KEEP_ROWS row) — 임시 보관 후 복원"

    # 업로더 employee 는 초기화로 사라지므로 FK 컬럼은 NULL (스키마의 ON DELETE SET NULL 과 동일 의미).
    KEEP_PRE_SQL="
CREATE TEMP TABLE _kept_app_package ON COMMIT DROP AS
  SELECT * FROM $TARGET_SCHEMA.$KEEP_TABLE;
UPDATE _kept_app_package SET uploaded_by_id = NULL;
"
    # identity 컬럼이라 값 복원에 OVERRIDING SYSTEM VALUE 가 필요하고,
    # CASCADE TRUNCATE 의 RESTART IDENTITY 로 시퀀스가 1 로 돌아가므로 setval 로 맞춰준다.
    KEEP_POST_SQL="
INSERT INTO $TARGET_SCHEMA.$KEEP_TABLE OVERRIDING SYSTEM VALUE
  SELECT * FROM _kept_app_package;
SELECT setval(pg_get_serial_sequence('$TARGET_SCHEMA.$KEEP_TABLE', 'app_package_id'),
              COALESCE(max(app_package_id), 1),
              max(app_package_id) IS NOT NULL)
  FROM $TARGET_SCHEMA.$KEEP_TABLE;
"
  else
    warn "$TARGET_SCHEMA.$KEEP_TABLE 테이블이 없어 보존 단계를 건너뜁니다."
  fi

  # 보관 → TRUNCATE → 복원이 한 트랜잭션이어야 중간 실패 시 데이터가 사라지지 않는다.
  "${PSQL[@]}" -v ON_ERROR_STOP=1 <<EOSQL
BEGIN;
$KEEP_PRE_SQL
DO \$do\$
DECLARE
  table_list text;
BEGIN
  SELECT string_agg(quote_ident(table_schema) || '.' || quote_ident(table_name), ', ')
    INTO table_list
  FROM information_schema.tables
  WHERE table_schema = '$TARGET_SCHEMA'
    AND table_type = 'BASE TABLE'
    AND table_name NOT IN ('flyway_schema_history', '$KEEP_TABLE');

  IF table_list IS NULL THEN
    RAISE NOTICE '$TARGET_SCHEMA 에 대상 테이블이 없습니다.';
  ELSE
    RAISE NOTICE '대상 테이블 목록: %', table_list;
    EXECUTE 'TRUNCATE TABLE ' || table_list || ' RESTART IDENTITY CASCADE';
    RAISE NOTICE 'TRUNCATE 완료';
  END IF;
END
\$do\$;
$KEEP_POST_SQL
COMMIT;
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
