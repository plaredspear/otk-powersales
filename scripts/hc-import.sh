#!/usr/bin/env bash
#
# hc-import.sh
#
# Heroku Connect DB → Dev DB 데이터 Import 스크립트
# HC DB(salesforce 스키마)에서 데이터를 추출하여 Dev DB(salesforce2 스키마)에 import한다.
#
# Usage:
#   ./scripts/hc-import.sh                         # account 테이블 UPSERT (기본)
#   ./scripts/hc-import.sh --dry-run                # SQL/행 수만 확인
#   ./scripts/hc-import.sh --truncate               # TRUNCATE 후 COPY
#   ./scripts/hc-import.sh --table account           # 테이블 지정
#   ./scripts/hc-import.sh --all                     # 모든 테이블 순차 import
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ACCOUNTS_FILE="$PROJECT_ROOT/docs/plan/old-accounts.json"

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
# 테이블별 설정 블록
################################################################################
# 새 테이블 추가 시: get_table_config 함수에 case 블록을 추가하면 됨.

SUPPORTED_TABLES="account"

# 테이블 설정을 반환하는 함수
# 출력: hc_schema|dev_schema|select_expr|dev_columns|conflict_key
get_table_config() {
  local table="$1"
  case "$table" in
    account)
      local hc_schema="salesforce2"
      local dev_schema="salesforce2"
      local select_expr="sfid, name, phone, mobilephone__c AS mobile_phone, address1__c AS address1, address2__c AS address2, representative__c AS representative, abctype__c AS abc_type, abctypecode__c AS abc_type_code, externalkey__c AS external_key, accountgroup__c AS account_group, branchcode__c AS branch_code, branchname__c AS branch_name, zipcode__c AS zip_code, latitude__c AS latitude, longitude__c AS longitude, closingtime1__c AS closing_time1, closingtime2__c AS closing_time2, closingtime3__c AS closing_time3, industry, werk1_tx__c AS werk1_tx, werk2_tx__c AS werk2_tx, werk3_tx__c AS werk3_tx, isdeleted AS is_deleted"
      local dev_columns="sfid, name, phone, mobile_phone, address1, address2, representative, abc_type, abc_type_code, external_key, account_group, branch_code, branch_name, zip_code, latitude, longitude, closing_time1, closing_time2, closing_time3, industry, werk1_tx, werk2_tx, werk3_tx, is_deleted"
      local conflict_key="sfid"
      echo "${hc_schema}|${dev_schema}|${select_expr}|${dev_columns}|${conflict_key}"
      ;;
    *)
      return 1
      ;;
  esac
}

################################################################################
# 인자 파싱
################################################################################

TABLE="account"
DRY_RUN=false
TRUNCATE=false
ALL=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --table)
      TABLE="$2"; shift 2 ;;
    --dry-run)
      DRY_RUN=true; shift ;;
    --truncate)
      TRUNCATE=true; shift ;;
    --all)
      ALL=true; shift ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  --table <name>  Import 대상 테이블 (기본: account)"
      echo "  --dry-run       실제 INSERT 없이 SQL/행 수만 출력"
      echo "  --truncate      Import 전 Dev DB 테이블 TRUNCATE (기본: UPSERT)"
      echo "  --all           등록된 모든 테이블 순차 import"
      echo "  --help          사용법 출력"
      echo ""
      echo "지원 테이블: $SUPPORTED_TABLES"
      exit 0
      ;;
    *) err "Unknown option: $1"; exit 1 ;;
  esac
done

# --all과 --table 동시 사용 방지 (--table 기본값이 아닌 경우)
if [[ "$ALL" == "true" && "$TABLE" != "account" ]]; then
  err "--all과 --table은 동시 사용할 수 없습니다."
  exit 1
fi

################################################################################
# 사전 검증
################################################################################

# jq 확인
if ! command -v jq &>/dev/null; then
  err "jq가 설치되어 있지 않습니다. brew install jq"
  exit 1
fi

# psql 확인
if ! command -v psql &>/dev/null; then
  err "psql이 설치되어 있지 않습니다."
  exit 1
fi

# old-accounts.json 확인
if [[ ! -f "$ACCOUNTS_FILE" ]]; then
  err "HC DB 접속 정보 파일을 찾을 수 없습니다: $ACCOUNTS_FILE"
  exit 1
fi

# HC DB 접속 정보 추출
HC_HOST=$(jq -r '.["dev-heroku-db"].HOST' "$ACCOUNTS_FILE")
HC_PORT=$(jq -r '.["dev-heroku-db"].PORT' "$ACCOUNTS_FILE")
HC_DB=$(jq -r '.["dev-heroku-db"].DATABASE' "$ACCOUNTS_FILE")
HC_USER=$(jq -r '.["dev-heroku-db"].USER' "$ACCOUNTS_FILE")
HC_PASS=$(jq -r '.["dev-heroku-db"].PASSWORD' "$ACCOUNTS_FILE")

# Dev DB 접속 정보
DEV_HOST="dev-db.codapt.kr"
DEV_PORT="5432"
DEV_DB="otoki"
DEV_USER="otoki_admin"

################################################################################
# DB 접속 테스트
################################################################################

log "=== DB 접속 테스트 ==="

if ! PGPASSWORD="$HC_PASS" PGSSLMODE=require psql -h "$HC_HOST" -p "$HC_PORT" -U "$HC_USER" -d "$HC_DB" \
  -c "SELECT 1;" &>/dev/null; then
  err "HC DB 접속 실패: $HC_HOST:$HC_PORT/$HC_DB"
  exit 1
fi
log "HC DB 접속 성공"

if ! psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
  -c "SELECT 1;" &>/dev/null; then
  err "Dev DB 접속 실패: $DEV_HOST:$DEV_PORT/$DEV_DB"
  exit 1
fi
log "Dev DB 접속 성공"

################################################################################
# Import 함수
################################################################################

import_table() {
  local table="$1"

  # 테이블 설정 조회
  local config
  if ! config=$(get_table_config "$table"); then
    err "지원하지 않는 테이블: $table"
    echo "지원 테이블: $SUPPORTED_TABLES"
    exit 1
  fi

  # 설정 파싱 (| 구분)
  local hc_schema dev_schema select_expr dev_columns conflict_key
  IFS='|' read -r hc_schema dev_schema select_expr dev_columns conflict_key <<< "$config"

  log "=== Import: $table ==="

  # HC DB 소스 행 수
  local hc_count
  hc_count=$(PGPASSWORD="$HC_PASS" PGSSLMODE=require psql -h "$HC_HOST" -p "$HC_PORT" -U "$HC_USER" -d "$HC_DB" \
    -t -A -c "SELECT count(*) FROM ${hc_schema}.${table};")
  log "HC DB 소스 행 수: $hc_count"

  # Dev DB 기존 행 수
  local dev_count_before
  dev_count_before=$(psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
    -t -A -c "SELECT count(*) FROM ${dev_schema}.${table};")
  log "Dev DB 기존 행 수: $dev_count_before"

  # Import 모드
  local mode="UPSERT"
  if [[ "$TRUNCATE" == "true" ]]; then
    mode="TRUNCATE"
  fi

  echo ""
  log "소스: ${hc_schema}.${table} ($hc_count rows)"
  log "타겟: ${dev_schema}.${table} ($dev_count_before rows)"
  log "모드: $mode"
  echo ""

  # SELECT SQL
  local select_sql="SELECT ${select_expr} FROM ${hc_schema}.${table}"

  # dry-run
  if [[ "$DRY_RUN" == "true" ]]; then
    log "[DRY-RUN] SELECT SQL:"
    echo "  $select_sql"
    log "[DRY-RUN] 예상 import 행 수: $hc_count"
    return 0
  fi

  # 사용자 확인
  if ! confirm "Import를 진행합니까?"; then
    log "사용자에 의해 취소되었습니다."
    exit 0
  fi

  if [[ "$TRUNCATE" == "true" ]]; then
    # TRUNCATE 모드: TRUNCATE + COPY
    log "Dev DB 테이블 TRUNCATE: ${dev_schema}.${table}"
    psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
      -c "TRUNCATE ${dev_schema}.${table} CASCADE;"

    log "데이터 전송 중 (COPY)..."
    PGPASSWORD="$HC_PASS" PGSSLMODE=require psql -h "$HC_HOST" -p "$HC_PORT" -U "$HC_USER" -d "$HC_DB" \
      -c "COPY (${select_sql}) TO STDOUT WITH (FORMAT csv, HEADER)" \
    | psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
      -c "COPY ${dev_schema}.${table}(${dev_columns}) FROM STDIN WITH (FORMAT csv, HEADER)"
  else
    # UPSERT 모드: 임시 테이블 → INSERT ON CONFLICT
    log "임시 테이블 생성 중..."
    psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
      -c "DROP TABLE IF EXISTS _tmp_import; CREATE TEMP TABLE _tmp_import AS SELECT * FROM ${dev_schema}.${table} WHERE false;"

    log "데이터 전송 중 (COPY → 임시 테이블)..."
    PGPASSWORD="$HC_PASS" PGSSLMODE=require psql -h "$HC_HOST" -p "$HC_PORT" -U "$HC_USER" -d "$HC_DB" \
      -c "COPY (${select_sql}) TO STDOUT WITH (FORMAT csv, HEADER)" \
    | psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
      -c "COPY _tmp_import(${dev_columns}) FROM STDIN WITH (FORMAT csv, HEADER)"

    # UPSERT SQL 생성: dev_columns에서 conflict_key 제외한 SET 절
    local update_set=""
    local OLD_IFS="$IFS"
    IFS=','
    for col in $dev_columns; do
      col=$(echo "$col" | xargs)  # trim whitespace
      if [[ "$col" != "$conflict_key" ]]; then
        if [[ -n "$update_set" ]]; then
          update_set="$update_set, "
        fi
        update_set="${update_set}${col} = EXCLUDED.${col}"
      fi
    done
    IFS="$OLD_IFS"

    log "UPSERT 실행 중..."
    psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" -c \
      "INSERT INTO ${dev_schema}.${table}(${dev_columns})
       SELECT ${dev_columns} FROM _tmp_import
       ON CONFLICT(${conflict_key}) DO UPDATE SET ${update_set};"

    log "임시 테이블 정리..."
    psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
      -c "DROP TABLE IF EXISTS _tmp_import;" 2>/dev/null || true
  fi

  # Import 후 행 수
  local dev_count_after
  dev_count_after=$(psql -h "$DEV_HOST" -p "$DEV_PORT" -U "$DEV_USER" -d "$DEV_DB" \
    -t -A -c "SELECT count(*) FROM ${dev_schema}.${table};")

  local diff=$((dev_count_after - dev_count_before))
  log "Import 완료: ${dev_schema}.${table}"
  log "  import 전: $dev_count_before rows → import 후: $dev_count_after rows (변동: ${diff})"
}

################################################################################
# 메인 실행
################################################################################

if [[ "$ALL" == "true" ]]; then
  for t in $SUPPORTED_TABLES; do
    import_table "$t"
  done
else
  import_table "$TABLE"
fi

log "=== 완료 ==="
