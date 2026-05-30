#!/usr/bin/env bash
#
# count-rows.sh
#
# SF org 의 각 SObject 의 row count 를 조회 (`SELECT COUNT() FROM <sobject>`).
# extract-csv.sh 의 TARGET_SPECS (common.kts) 와 동일한 SObject 목록을 사용.
#
# IsDeleted=true row 는 기본 제외 (COUNT() 가 active rows 만 집계).
# --include-deleted 옵션 사용 시 COUNT(Id) + soft-deleted 포함 query.
#
# Usage:
#   scripts/sf-data-migration/count-rows.sh [--org <alias>] [--target=<list>] [--include-deleted] [--api-version <ver>]
#
# 예:
#   scripts/sf-data-migration/count-rows.sh
#   scripts/sf-data-migration/count-rows.sh --org otoki-prod
#   scripts/sf-data-migration/count-rows.sh --target=InspectionTheme,AttendanceLog
#
set -euo pipefail

# -----------------------------------------------------------------------------
# 인자 파싱
# -----------------------------------------------------------------------------

SF_ORG=""
SF_API_VERSION="60.0"
TARGETS=""
INCLUDE_DELETED=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --org)              SF_ORG="$2"; shift 2 ;;
        --api-version)      SF_API_VERSION="$2"; shift 2 ;;
        --target=*)         TARGETS="${1#--target=}"; shift ;;
        --include-deleted)  INCLUDE_DELETED=1; shift ;;
        -h|--help)
            sed -n '2,18p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown arg: $1" >&2
            echo "Usage: $0 [--org <alias>] [--target=<list>] [--include-deleted] [--api-version <ver>]" >&2
            exit 1
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMMON_KTS="$SCRIPT_DIR/common.kts"

# -----------------------------------------------------------------------------
# 사전 검증
# -----------------------------------------------------------------------------

if ! command -v sf >/dev/null 2>&1; then
    echo "[ERROR] 'sf' CLI not found. Install: npm i -g @salesforce/cli" >&2
    exit 1
fi

if [[ ! -f "$COMMON_KTS" ]]; then
    echo "[ERROR] common.kts not found: $COMMON_KTS" >&2
    exit 1
fi

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

# -----------------------------------------------------------------------------
# common.kts 에서 target ↔ sObject 매핑 추출
# -----------------------------------------------------------------------------
# 형식: "<target>"<space>to<space>TargetSpec(<META>, "<sObjectName>", ...)
# 출력: <target><TAB><sObjectName>

MAPPING="$(/usr/bin/grep -oE '"[A-Za-z]+"[[:space:]]+to[[:space:]]+TargetSpec\([A-Z_]+_METADATA,[[:space:]]+"[^"]+"' "$COMMON_KTS" \
    | /usr/bin/sed -E 's/"([A-Za-z]+)"[[:space:]]+to[[:space:]]+TargetSpec\([A-Z_]+_METADATA,[[:space:]]+"([^"]+)"/\1	\2/')"

if [[ -z "$MAPPING" ]]; then
    echo "[ERROR] common.kts 에서 target ↔ sObject 매핑을 추출하지 못함." >&2
    exit 1
fi

# --target 필터 적용 (콤마 구분)
if [[ -n "$TARGETS" ]]; then
    FILTERED=""
    IFS=',' read -ra REQ_TARGETS <<< "$TARGETS"
    while IFS=$'\t' read -r target sobject; do
        for req in "${REQ_TARGETS[@]}"; do
            if [[ "$req" == "$target" ]]; then
                FILTERED+="$target	$sobject"$'\n'
                break
            fi
        done
    done <<< "$MAPPING"
    MAPPING="${FILTERED%$'\n'}"
fi

TOTAL_TARGETS=$(echo "$MAPPING" | /usr/bin/wc -l | /usr/bin/tr -d ' ')

echo "============================================================"
echo "SF SObject row count 조회"
echo "============================================================"
echo "[info] sf CLI    : $(sf --version | /usr/bin/head -1)"
echo "[info] org       : ${SF_ORG:-"(default — sf config set target-org)"}"
echo "[info] api ver   : $SF_API_VERSION"
echo "[info] targets   : $TOTAL_TARGETS"
echo "[info] deleted   : $([ "$INCLUDE_DELETED" -eq 1 ] && echo "포함 (queryAll)" || echo "제외 (active only)")"
echo

# -----------------------------------------------------------------------------
# 각 SObject 의 row count 조회
# -----------------------------------------------------------------------------

printf "%-44s  %-44s  %12s\n" "Target" "SObject" "Row Count"
printf "%-44s  %-44s  %12s\n" "------" "-------" "---------"

GRAND_TOTAL=0
FAILED=()

# queryAll 옵션 — soft-deleted 포함 시 사용
QUERY_FLAGS=()
if [[ "$INCLUDE_DELETED" -eq 1 ]]; then
    QUERY_FLAGS+=(--use-tooling-api=false)
fi

while IFS=$'\t' read -r target sobject; do
    [[ -z "$target" ]] && continue
    if [[ "$INCLUDE_DELETED" -eq 1 ]]; then
        soql="SELECT COUNT(Id) total FROM $sobject"
    else
        soql="SELECT COUNT(Id) total FROM $sobject WHERE IsDeleted = false"
    fi

    # SF CLI json 결과 parsing — totalSize 가 1 (단일 aggregate row), records[0].total 이 실제 카운트
    result=$(sf data query \
        --query "$soql" \
        --result-format json \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}" 2>/dev/null || echo "")

    if [[ -z "$result" ]]; then
        # IsDeleted 가 없는 SObject (Group / User 등) 는 fallback — WHERE 절 제거 재시도
        soql_fallback="SELECT COUNT(Id) total FROM $sobject"
        result=$(sf data query \
            --query "$soql_fallback" \
            --result-format json \
            --api-version "$SF_API_VERSION" \
            "${SF_ORG_ARGS[@]}" 2>/dev/null || echo "")
    fi

    if [[ -z "$result" ]]; then
        printf "%-44s  %-44s  %12s\n" "$target" "$sobject" "FAILED"
        FAILED+=("$target ($sobject)")
        continue
    fi

    count=$(echo "$result" | /usr/bin/python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('result',{}).get('records',[{}])[0].get('total', d.get('result',{}).get('totalSize', '?')))" 2>/dev/null || echo "?")

    printf "%-44s  %-44s  %12s\n" "$target" "$sobject" "$count"

    if [[ "$count" =~ ^[0-9]+$ ]]; then
        GRAND_TOTAL=$((GRAND_TOTAL + count))
    fi
done <<< "$MAPPING"

echo
echo "============================================================"
printf "%-44s  %-44s  %12s\n" "" "GRAND TOTAL" "$GRAND_TOTAL"
echo "============================================================"

if [[ ${#FAILED[@]} -gt 0 ]]; then
    echo
    echo "[warn] 조회 실패 (${#FAILED[@]} 건):"
    for f in "${FAILED[@]}"; do
        echo "  - $f"
    done
fi
