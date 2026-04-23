#!/usr/bin/env bash
# SSM Session Manager 포트 포워딩으로 private subnet RDS 에 접속하는 래퍼.
# 끊어지면 자동 재연결하며, Ctrl+C 로 종료.
set -euo pipefail

PROJECT="otk-pwrs"
LOCAL_PORT="15432"
AWS_PROFILE="${AWS_PROFILE:-}"
PRINT_PASSWORD=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -p <profile>    AWS CLI profile (dev-otk-pwrs-db-access | prod-otk-pwrs-db-access)
  -l <port>       local port                (default: 15432)
  --password      RDS master password 만 출력하고 종료
  -h              도움말

Examples:
  $(basename "$0") -p dev-otk-pwrs-db-access
  $(basename "$0") -p prod-otk-pwrs-db-access -l 25432
  $(basename "$0") -p dev-otk-pwrs-db-access --password
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p) AWS_PROFILE="$2"; shift 2;;
    -l) LOCAL_PORT="$2"; shift 2;;
    --password) PRINT_PASSWORD=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown option: $1" >&2; usage; exit 1;;
  esac
done

ALLOWED_PROFILES=("dev-otk-pwrs-db-access" "prod-otk-pwrs-db-access")
if [[ -z "$AWS_PROFILE" ]] || [[ ! " ${ALLOWED_PROFILES[*]} " =~ " ${AWS_PROFILE} " ]]; then
  echo "Error: -p 옵션으로 profile 을 지정해야 합니다." >&2
  echo "Allowed: ${ALLOWED_PROFILES[*]}" >&2
  exit 1
fi

# profile 이름에서 stage 자동 추출 (dev-otk-pwrs-db-access → dev)
STAGE="${AWS_PROFILE%%-otk-pwrs-db-access}"

aws_() { aws --profile "$AWS_PROFILE" --region ap-northeast-2 "$@"; }

ENV_FILE="$(dirname "$0")/db-tunnel.${STAGE}.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: .env 파일을 찾을 수 없습니다: $ENV_FILE" >&2
  exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

if [[ "$PRINT_PASSWORD" -eq 1 ]]; then
  aws_ secretsmanager get-secret-value \
    --secret-id "$RDS_SECRET_ARN" \
    --query 'SecretString' --output text | jq -r .password
  exit 0
fi

echo "==> EB 인스턴스 조회 (tag: Project=$PROJECT, Stage=$STAGE)..."
EB_INSTANCE=$(aws_ ec2 describe-instances \
  --filters "Name=tag:Project,Values=$PROJECT" \
            "Name=tag:Stage,Values=$STAGE" \
            "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[0].InstanceId' \
  --output text | awk '{print $1}')

if [[ -z "$EB_INSTANCE" || "$EB_INSTANCE" == "None" ]]; then
  echo "Error: 실행 중인 EB 인스턴스를 찾을 수 없습니다." >&2
  exit 1
fi

cat <<EOF

--------------------------------------------------------------------
  Stage         : $STAGE
  AWS Profile   : $AWS_PROFILE
  EB Instance   : $EB_INSTANCE
  RDS Host      : $RDS_HOST
  RDS Port      : $RDS_PORT
  Local Port    : $LOCAL_PORT

  DB 클라이언트 연결 정보
    Host        : localhost
    Port        : $LOCAL_PORT
    Username    : $RDS_USER
    Password    : $(basename "$0") --password -s $STAGE   ← 로 조회

  세션 종료: Ctrl+C
--------------------------------------------------------------------

EOF

cleanup() {
  echo ""
  echo "==> 터널 종료."
  exit 0
}
trap cleanup INT TERM

while true; do
  aws_ ssm start-session \
    --target "$EB_INSTANCE" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"host\":[\"$RDS_HOST\"],\"portNumber\":[\"$RDS_PORT\"],\"localPortNumber\":[\"$LOCAL_PORT\"]}" \
    || true
  echo ""
  echo "==> 세션이 끊어졌습니다. 2초 후 재연결..." >&2
  sleep 2
done
