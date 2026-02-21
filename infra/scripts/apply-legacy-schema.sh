#!/usr/bin/env bash
#
# apply-legacy-schema.sh
#
# RDS(private subnet)에 레거시 파워세일즈 스키마를 적용합니다.
# 1) RDS를 임시로 publicly accessible로 변경
# 2) Security Group에 현재 IP 추가
# 3) psql로 스키마 적용
# 4) 원복 (에러/중단 시에도 자동 원복)
#
# Usage:
#   ./apply-legacy-schema.sh                          # 기본값 사용
#   ./apply-legacy-schema.sh --db-identifier otoki-dev-db --sql-file /path/to/schema.sql
#
set -euo pipefail

################################################################################
# 설정
################################################################################

DB_IDENTIFIER="otoki-dev-db"
DB_NAME="otoki"
DB_USER="otoki_admin"
SCHEMA_NAME="salesforce2"
LEGACY_DB_USER="u4bee3ek26k44g"

# 스크립트 위치 기준 상대 경로로 SQL 파일 탐색
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/../../../docs/plan/기존소스/파워세일즈 스키마.SQL"

WAIT_TIMEOUT=120  # RDS 변경 대기 최대 시간 (초)
WAIT_INTERVAL=5   # 폴링 간격 (초)

# 원복 추적 변수
SG_RULE_ADDED=false
RDS_MADE_PUBLIC=false
SG_ID=""
RULE_ID=""

################################################################################
# 인자 파싱
################################################################################

while [[ $# -gt 0 ]]; do
  case $1 in
    --db-identifier) DB_IDENTIFIER="$2"; shift 2 ;;
    --db-name)       DB_NAME="$2"; shift 2 ;;
    --db-user)       DB_USER="$2"; shift 2 ;;
    --sql-file)      SQL_FILE="$2"; shift 2 ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo "  --db-identifier  RDS instance identifier (default: otoki-dev-db)"
      echo "  --db-name        Database name (default: otoki)"
      echo "  --db-user        Database user (default: otoki_admin)"
      echo "  --sql-file       Path to legacy SQL file"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

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
# 원복 (trap)
################################################################################

cleanup() {
  local exit_code=$?
  echo ""
  log "=== 원복 시작 ==="

  if [[ "$SG_RULE_ADDED" == "true" && -n "$SG_ID" ]]; then
    # 태그로 규칙 ID 조회
    RULE_ID=$(aws ec2 describe-security-group-rules \
      --filters "Name=group-id,Values=$SG_ID" "Name=tag:Name,Values=temp-schema-import" \
      --query 'SecurityGroupRules[0].SecurityGroupRuleId' \
      --output text 2>/dev/null || true)

    if [[ -n "$RULE_ID" && "$RULE_ID" != "None" ]]; then
      log "Security Group 임시 규칙 제거: $RULE_ID"
      aws ec2 revoke-security-group-ingress \
        --group-id "$SG_ID" \
        --security-group-rule-ids "$RULE_ID" 2>/dev/null || warn "SG 규칙 제거 실패 — 수동 제거 필요: $RULE_ID"
    fi
  fi

  if [[ "$RDS_MADE_PUBLIC" == "true" ]]; then
    log "RDS private 복원 중..."
    aws rds modify-db-instance \
      --db-instance-identifier "$DB_IDENTIFIER" \
      --no-publicly-accessible \
      --apply-immediately >/dev/null 2>&1 || warn "RDS private 복원 실패 — 수동 복원 필요"
    log "RDS private 복원 요청 완료 (반영까지 1~2분 소요)"
  fi

  if [[ $exit_code -eq 0 ]]; then
    log "=== 원복 완료 ==="
  else
    warn "=== 스크립트가 비정상 종료되었으나 원복은 수행되었습니다 ==="
  fi

  exit $exit_code
}

trap cleanup EXIT INT TERM

################################################################################
# 사전 검증
################################################################################

log "=== 사전 검증 ==="

# 필수 도구 확인
for cmd in aws psql sed curl; do
  if ! command -v "$cmd" &>/dev/null; then
    err "'$cmd'이 설치되어 있지 않습니다."
    exit 1
  fi
done

# SQL 파일 존재 확인
if [[ ! -f "$SQL_FILE" ]]; then
  err "SQL 파일을 찾을 수 없습니다: $SQL_FILE"
  exit 1
fi

# AWS 인증 확인
if ! aws sts get-caller-identity &>/dev/null; then
  err "AWS 인증이 설정되어 있지 않습니다. aws configure 또는 AWS_PROFILE을 확인하세요."
  exit 1
fi

# RDS 인스턴스 존재 확인
if ! aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" &>/dev/null; then
  err "RDS 인스턴스를 찾을 수 없습니다: $DB_IDENTIFIER"
  exit 1
fi

# 현재 상태 확인
CURRENT_PUBLIC=$(aws rds describe-db-instances \
  --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].PubliclyAccessible' \
  --output text)

RDS_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

SG_ID=$(aws rds describe-db-instances \
  --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
  --output text)

MY_IP=$(curl -s ifconfig.me)

SQL_LINES=$(wc -l < "$SQL_FILE" | tr -d ' ')

log "RDS Instance:     $DB_IDENTIFIER"
log "RDS Host:         $RDS_HOST"
log "Database:         $DB_NAME"
log "User:             $DB_USER"
log "Security Group:   $SG_ID"
log "현재 Public 여부: $CURRENT_PUBLIC"
log "내 공인 IP:       $MY_IP"
log "SQL 파일:         $SQL_FILE ($SQL_LINES lines)"
echo ""

if ! confirm "위 설정으로 레거시 스키마를 적용합니까?"; then
  log "사용자에 의해 취소되었습니다."
  exit 0
fi

################################################################################
# Step 1: RDS 임시 public 접근 허용
################################################################################

log "=== Step 1: RDS public 접근 허용 ==="

if [[ "$CURRENT_PUBLIC" == "True" ]]; then
  log "RDS가 이미 publicly accessible 상태입니다. 건너뜁니다."
else
  log "RDS publicly accessible 변경 중..."
  aws rds modify-db-instance \
    --db-instance-identifier "$DB_IDENTIFIER" \
    --publicly-accessible \
    --apply-immediately >/dev/null
  RDS_MADE_PUBLIC=true

  # 변경 완료 대기
  log "RDS 변경 대기 중 (최대 ${WAIT_TIMEOUT}초)..."
  elapsed=0
  while [[ $elapsed -lt $WAIT_TIMEOUT ]]; do
    status=$(aws rds describe-db-instances \
      --db-instance-identifier "$DB_IDENTIFIER" \
      --query 'DBInstances[0].PubliclyAccessible' \
      --output text)
    if [[ "$status" == "True" ]]; then
      log "RDS publicly accessible 변경 완료"
      break
    fi
    sleep "$WAIT_INTERVAL"
    elapsed=$((elapsed + WAIT_INTERVAL))
    printf "\r  대기 중... %ds / %ds" "$elapsed" "$WAIT_TIMEOUT"
  done
  echo ""

  if [[ $elapsed -ge $WAIT_TIMEOUT ]]; then
    err "RDS 변경 타임아웃 (${WAIT_TIMEOUT}초). 수동 확인이 필요합니다."
    exit 1
  fi

  # RDS 호스트 갱신 (public IP가 할당되면 endpoint가 바뀔 수 있음)
  RDS_HOST=$(aws rds describe-db-instances \
    --db-instance-identifier "$DB_IDENTIFIER" \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)
fi

# Security Group에 내 IP 추가
log "Security Group에 내 IP 추가: ${MY_IP}/32"
aws ec2 authorize-security-group-ingress \
  --group-id "$SG_ID" \
  --protocol tcp \
  --port 5432 \
  --cidr "${MY_IP}/32" \
  --tag-specifications 'ResourceType=security-group-rule,Tags=[{Key=Name,Value=temp-schema-import}]' >/dev/null
SG_RULE_ADDED=true

################################################################################
# Step 2: 스키마 적용
################################################################################

log "=== Step 2: 스키마 적용 ==="

# 접속 테스트 (최대 30초 재시도)
log "RDS 접속 테스트 중..."
connect_ok=false
for i in {1..6}; do
  if psql -h "$RDS_HOST" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" &>/dev/null; then
    connect_ok=true
    break
  fi
  log "  접속 재시도... ($i/6)"
  sleep 5
done

if [[ "$connect_ok" != "true" ]]; then
  err "RDS 접속 실패. 비밀번호를 확인하세요 (PGPASSWORD 환경변수 또는 ~/.pgpass 설정)."
  err "  수동 테스트: psql -h $RDS_HOST -U $DB_USER -d $DB_NAME"
  exit 1
fi

log "RDS 접속 성공"

# salesforce2 스키마 생성
log "스키마 생성: $SCHEMA_NAME"
psql -h "$RDS_HOST" -U "$DB_USER" -d "$DB_NAME" \
  -c "CREATE SCHEMA IF NOT EXISTS $SCHEMA_NAME AUTHORIZATION $DB_USER;"

# 레거시 SQL 적용
log "레거시 스키마 적용 중 ($SQL_LINES lines)..."
sed \
  -e "s/${LEGACY_DB_USER}/${DB_USER}/g" \
  -e "/^CREATE SCHEMA ${SCHEMA_NAME}/d" \
  "$SQL_FILE" \
  | psql -h "$RDS_HOST" -U "$DB_USER" -d "$DB_NAME" --quiet --set ON_ERROR_STOP=off 2>&1 \
  | grep -i "error" || true

log "스키마 적용 완료"

################################################################################
# Step 3: 적용 확인
################################################################################

log "=== Step 3: 적용 확인 ==="

TABLE_COUNT=$(psql -h "$RDS_HOST" -U "$DB_USER" -d "$DB_NAME" -t -A \
  -c "SELECT count(*) FROM information_schema.tables WHERE table_schema = '$SCHEMA_NAME';")

log "생성된 테이블 수: $TABLE_COUNT"

# 주요 테이블 존재 확인
log "주요 테이블 확인:"
psql -h "$RDS_HOST" -U "$DB_USER" -d "$DB_NAME" -t -A \
  -c "SELECT '  - ' || table_name FROM information_schema.tables
      WHERE table_schema = '$SCHEMA_NAME'
        AND table_name IN ('dkretail__employee__c', 'employee_mng', 'account')
      ORDER BY table_name;"

if [[ "$TABLE_COUNT" -eq 0 ]]; then
  warn "테이블이 0개입니다. 스키마 적용에 문제가 있을 수 있습니다."
  exit 1
fi

echo ""
log "=== 스키마 적용 성공 (테이블 ${TABLE_COUNT}개) ==="
log "원복이 자동으로 진행됩니다..."
