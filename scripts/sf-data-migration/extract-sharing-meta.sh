#!/usr/bin/env bash
#
# SF Sharing 정책 메타 CSV 추출 (spec #782 P1-B).
#
# `sf data query` 가 아닌 **retrieve 메타 파일 (sharingRules / role / profile / permissionset XML) 출처**.
# 본 스크립트는 extract-sharing-meta.main.kts 의 bash wrapper.
#
# 사용법:
#   ./extract-sharing-meta.sh [--src-dir <retrieve 루트>] [--out-dir <CSV 출력>]
#
#   --src-dir : retrieve 루트 (기본: docs/plan/old_source_260516/aladdin_260516_prod/)
#   --out-dir : CSV 출력 디렉토리 (기본: ./input/)
#
# 출력:
#   - input/sharing-rule.csv
#   - input/sharing-rule-condition.csv
#   - input/sharing-rule-target.csv
#   - input/user-role-hierarchy.csv
#   - input/profile-flags.csv
#   - input/permission-set-flags.csv
#
# 후속: migrate-stage1.main.kts 가 본 CSV 를 적재 + Stage 2 fk substep 이 sfid → _id resolve.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

SRC_DIR="$PROJECT_ROOT/docs/plan/old_source_260516/aladdin_260516_prod"
OUT_DIR="$SCRIPT_DIR/input"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --src-dir) SRC_DIR="$2"; shift 2 ;;
        --out-dir) OUT_DIR="$2"; shift 2 ;;
        -h|--help)
            sed -n '1,30p' "$0"
            exit 0
            ;;
        *) echo "Unknown arg: $1" >&2; exit 1 ;;
    esac
done

if [[ ! -d "$SRC_DIR/force-app/main/default" ]]; then
    echo "[ERROR] retrieve 루트가 아닙니다: $SRC_DIR (force-app/main/default 부재)" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"

echo "[extract-sharing-meta] src=$SRC_DIR out=$OUT_DIR"

kotlinc -script "$SCRIPT_DIR/extract-sharing-meta.main.kts" -- \
    --src-dir "$SRC_DIR" \
    --out-dir "$OUT_DIR"
