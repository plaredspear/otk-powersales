#!/usr/bin/env bash
# SSM Session Manager 를 통해 private subnet EC2 에 SSH 또는 shell 세션으로 접속.
set -euo pipefail

PROJECT="otk-pwrs"
STAGE=""
SSH_KEY=""
SSH_USER="ec2-user"
USE_SHELL=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -s <stage>      stage (dev | prod)
  -i <key.pem>    SSH 키 파일 경로 (생략 시 SSM shell 세션으로 접속)
  -u <user>       SSH 사용자 (default: ec2-user)
  --shell         SSH 없이 SSM shell 세션으로 접속
  -h              도움말

Examples:
  $(basename "$0") -s dev                        # SSM shell 세션
  $(basename "$0") -s dev -i ~/.ssh/key.pem      # SSH over SSM
  $(basename "$0") -s prod --shell               # SSM shell 세션
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s) STAGE="$2"; shift 2;;
    -i) SSH_KEY="$2"; shift 2;;
    -u) SSH_USER="$2"; shift 2;;
    --shell) USE_SHELL=1; shift;;
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

AWS_PROFILE="${STAGE}-otk-pwrs-db-access"
aws_() { aws --profile "$AWS_PROFILE" --region ap-northeast-2 "$@"; }

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
  접속 방식     : $(if [[ -n "$SSH_KEY" && "$USE_SHELL" -eq 0 ]]; then echo "SSH over SSM ($SSH_USER)"; else echo "SSM shell 세션"; fi)
--------------------------------------------------------------------

EOF

if [[ -n "$SSH_KEY" && "$USE_SHELL" -eq 0 ]]; then
  ssh -i "$SSH_KEY" \
    -o StrictHostKeyChecking=no \
    -o "ProxyCommand=sh -c 'aws ssm start-session --target %h --document-name AWS-StartSSHSession --parameters portNumber=%p --profile $AWS_PROFILE --region ap-northeast-2'" \
    "${SSH_USER}@${EB_INSTANCE}"
else
  aws_ ssm start-session --target "$EB_INSTANCE"
fi
