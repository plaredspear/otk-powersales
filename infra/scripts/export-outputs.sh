#!/usr/bin/env bash
#
# export-outputs.sh — SSM Parameter Store에서 인프라 outputs를 가져와
# JSON 파일과 backend .env 파일을 생성합니다.
#
# Usage:
#   ./export-outputs.sh [project] [environment]
#   ./export-outputs.sh otoki dev
#
set -euo pipefail

PROJECT="${1:-otoki}"
ENVIRONMENT="${2:-dev}"
SSM_PATH="/${PROJECT}/${ENVIRONMENT}/infra"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/../outputs"

mkdir -p "$OUTPUT_DIR"

echo "Fetching SSM parameters from ${SSM_PATH} ..."

# Fetch all parameters under the path
PARAMS=$(aws ssm get-parameters-by-path \
  --path "$SSM_PATH" \
  --query "Parameters[].{Name:Name,Value:Value}" \
  --output json)

# Save raw JSON
echo "$PARAMS" > "${OUTPUT_DIR}/infra-outputs.json"
echo "Saved: ${OUTPUT_DIR}/infra-outputs.json"

# Generate backend .env
ENV_FILE="${OUTPUT_DIR}/backend.env"
: > "$ENV_FILE"

echo "$PARAMS" | python3 -c "
import json, sys
params = json.load(sys.stdin)
mapping = {
    'rds-address':          'DB_HOST',
    'rds-port':             'DB_PORT',
    'elasticache-endpoint': 'REDIS_HOST',
    'elasticache-port':     'REDIS_PORT',
    'api-url':              'API_URL',
    'nat-gateway-ip':       'NAT_GATEWAY_IP',
}
for p in params:
    key = p['Name'].rsplit('/', 1)[-1]
    env_key = mapping.get(key)
    if env_key:
        print(f\"{env_key}={p['Value']}\")
" >> "$ENV_FILE"

echo "Saved: ${ENV_FILE}"
echo "Done."
