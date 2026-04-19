#!/usr/bin/env bash
# SSM Session Manager 포트 포워딩으로 private subnet RDS 에 접속하는 래퍼.
# 끊어지면 자동 재연결하며, Ctrl+C 로 종료.
set -euo pipefail

PROJECT="otk-pwrs"
STAGE="dev"
LOCAL_PORT="15432"
AWS_PROFILE="${AWS_PROFILE:-dev-codapt}"
PRINT_PASSWORD=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -s <stage>      dev | prod                (default: dev)
  -l <port>       local port                (default: 15432)
  -p <profile>    AWS CLI profile           (default: \$AWS_PROFILE or dev-codapt)
  --password      RDS master password 만 출력하고 종료
  -h              도움말

Examples:
  $(basename "$0")                          # dev 환경, localhost:15432
  $(basename "$0") -s prod -l 25432         # prod 환경, localhost:25432
  $(basename "$0") --password               # 비밀번호만 조회
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s) STAGE="$2"; shift 2;;
    -l) LOCAL_PORT="$2"; shift 2;;
    -p) AWS_PROFILE="$2"; shift 2;;
    --password) PRINT_PASSWORD=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown option: $1" >&2; usage; exit 1;;
  esac
done

if [[ "$STAGE" != "dev" && "$STAGE" != "prod" ]]; then
  echo "Error: stage 는 dev 또는 prod 여야 합니다 (given: $STAGE)" >&2
  exit 1
fi

aws_() { aws --profile "$AWS_PROFILE" --region ap-northeast-2 "$@"; }

echo "==> SSM Parameter Store 에서 설정 조회 (stage=$STAGE)..."
RDS_HOST=$(aws_ ssm get-parameter \
  --name "/$PROJECT/$STAGE/rds/host" \
  --query 'Parameter.Value' --output text)
RDS_PORT=$(aws_ ssm get-parameter \
  --name "/$PROJECT/$STAGE/rds/port" \
  --query 'Parameter.Value' --output text)
RDS_USER=$(aws_ ssm get-parameter \
  --name "/$PROJECT/$STAGE/rds/username" \
  --query 'Parameter.Value' --output text)
RDS_SECRET_ARN=$(aws_ ssm get-parameter \
  --name "/$PROJECT/$STAGE/rds/secret-arn" \
  --query 'Parameter.Value' --output text)

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
