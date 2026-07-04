#!/usr/bin/env bash
# SSM Session Manager 포트 포워딩으로 private subnet RDS 에 접속하는 래퍼.
# 끊어지면 자동 재연결하며, Ctrl+C 로 종료.
set -euo pipefail

PROJECT="otk-pwrs"
LOCAL_PORT=""
STAGE=""
PRINT_PASSWORD=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -s <stage>      stage (dev | prod)
  -l <port>       local port                (default: dev=15432, prod=25432)
  --password      RDS master password 만 출력하고 종료 (Secrets Manager 에서 조회)
  -h              도움말

Examples:
  $(basename "$0") -s dev
  $(basename "$0") -s prod -l 25432
  $(basename "$0") -s dev --password
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s) STAGE="$2"; shift 2;;
    -l) LOCAL_PORT="$2"; shift 2;;
    --password) PRINT_PASSWORD=1; shift;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown option: $1" >&2; usage; exit 1;;
  esac
done

ALLOWED_STAGES=("dev" "prod")
if [[ -z "$STAGE" ]] || [[ ! " ${ALLOWED_STAGES[*]} " =~ " ${STAGE} " ]]; then
  echo "Error: -s 옵션으로 stage 를 지정해야 합니다." >&2
  echo "Allowed: ${ALLOWED_STAGES[*]}" >&2
  exit 1
fi

# stage 별 기본 로컬 포트 (dev/prod 동시 실행 시 충돌 방지). -l 로 override 가능.
if [[ -z "$LOCAL_PORT" ]]; then
  case "$STAGE" in
    dev)  LOCAL_PORT=15432 ;;
    prod) LOCAL_PORT=25432 ;;
  esac
fi

# stage 별 AWS profile 자동 결정 (dev → dev-otk-pwrs-db-access)
AWS_PROFILE="${STAGE}-otk-pwrs-db-access"

aws_() { aws --profile "$AWS_PROFILE" --region ap-northeast-2 "$@"; }

ENV_FILE="$(dirname "$0")/db-tunnel.${STAGE}.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Error: .env 파일을 찾을 수 없습니다: $ENV_FILE" >&2
  exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

if [[ "$PRINT_PASSWORD" -eq 1 ]]; then
  if [[ -z "${RDS_SECRET_ARN:-}" ]]; then
    echo "Error: $ENV_FILE 에 RDS_SECRET_ARN 이 설정되어 있지 않습니다." >&2
    exit 1
  fi
  SECRET_JSON=$(aws_ secretsmanager get-secret-value \
    --secret-id "$RDS_SECRET_ARN" \
    --query SecretString --output text)
  PASSWORD_VALUE=$(printf '%s' "$SECRET_JSON" | jq -r '.password // empty')
  if [[ -z "$PASSWORD_VALUE" ]]; then
    echo "Error: Secrets Manager 응답에서 password 를 추출하지 못했습니다 (ARN: $RDS_SECRET_ARN)." >&2
    exit 1
  fi
  echo "$PASSWORD_VALUE"
  exit 0
fi

# 실행 중인 EB 인스턴스 ID 를 조회 (tag 필터). 인스턴스 교체(재배포/오토스케일링)
# 시 ID 가 바뀌므로, 시작 시 1회가 아니라 재연결 루프 안에서 매번 재조회한다.
# 롤링 배포 중 old+new 가 잠깐 공존할 수 있어, LaunchTime 최신 1대를 선택한다
# (Reservations[].Instances[0] 은 순서 보장이 없어 옛 인스턴스를 집을 수 있음).
lookup_eb_instance() {
  aws_ ec2 describe-instances \
    --filters "Name=tag:Project,Values=$PROJECT" \
              "Name=tag:Stage,Values=$STAGE" \
              "Name=instance-state-name,Values=running" \
    --query 'sort_by(Reservations[].Instances[], &LaunchTime)[-1].InstanceId' \
    --output text | awk '{print $1}'
}

echo "==> EB 인스턴스 조회 (tag: Project=$PROJECT, Stage=$STAGE)..."
EB_INSTANCE=$(lookup_eb_instance)

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
    Password    : $(basename "$0") -s $STAGE --password   ← 로 조회 (환경변수 필요)

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

  # 인스턴스 교체(재배포/오토스케일링) 시 ID 가 바뀌므로 재연결마다 재조회.
  NEW_INSTANCE=$(lookup_eb_instance)
  if [[ -n "$NEW_INSTANCE" && "$NEW_INSTANCE" != "None" && "$NEW_INSTANCE" != "$EB_INSTANCE" ]]; then
    echo "==> EB 인스턴스 변경 감지: $EB_INSTANCE -> $NEW_INSTANCE" >&2
    EB_INSTANCE="$NEW_INSTANCE"
  fi
done
