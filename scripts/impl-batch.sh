#!/usr/bin/env bash
#
# impl-batch.sh - 승인된 스펙을 연속으로 구현하는 배치 오케스트레이터
#
# Usage:
#   ./scripts/impl-batch.sh          # 기본: 최대 50회 반복
#   ./scripts/impl-batch.sh 10       # 최대 10회 반복
#
# 동작:
#   1. claude -p "/impl-next" --dangerously-skip-permissions 실행
#   2. 상태 마커 파싱 (SUCCESS / NO_SPECS / FAILED)
#   3. SUCCESS → 다음 반복, FAILED/NO_SPECS → 종료
#   4. 배치 요약 출력

set -euo pipefail

# ─── 설정 ───────────────────────────────────────────────────────────

MAX_ITERATIONS="${1:-50}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_DIR="$PROJECT_ROOT/docs/execution/impl-batch-logs"
BATCH_LOG="$LOG_DIR/batch-$TIMESTAMP.log"
LOCKFILE="$PROJECT_ROOT/.impl-batch.lock"

# ─── 함수 ───────────────────────────────────────────────────────────

log() {
    local msg="[$(date '+%H:%M:%S')] $1"
    echo "$msg"
    echo "$msg" >> "$BATCH_LOG"
}

cleanup() {
    rm -f "$LOCKFILE"
    log "Lockfile removed."
}

print_summary() {
    echo ""
    echo "============================================"
    echo "  impl-batch 배치 요약"
    echo "============================================"
    echo "  시작 시각: $TIMESTAMP"
    echo "  종료 시각: $(date +%Y%m%d-%H%M%S)"
    echo "  총 반복:   $iteration / $MAX_ITERATIONS"
    echo "  성공:      ${#completed[@]}"
    if [ ${#completed[@]} -gt 0 ]; then
        echo "  완료 스펙:"
        for spec in "${completed[@]}"; do
            echo "    - $spec"
        done
    fi
    if [ -n "$fail_reason" ]; then
        echo "  실패:      $fail_spec ($fail_reason)"
    fi
    echo "  로그 디렉토리: $LOG_DIR"
    echo "============================================"
}

# ─── 사전 점검 ──────────────────────────────────────────────────────

# Lockfile 중복 실행 방지
if [ -f "$LOCKFILE" ]; then
    existing_pid=$(cat "$LOCKFILE" 2>/dev/null || echo "unknown")
    echo "ERROR: impl-batch가 이미 실행 중입니다 (PID: $existing_pid)"
    echo "강제 종료하려면: rm $LOCKFILE"
    exit 1
fi

# claude CLI 존재 확인
if ! command -v claude &> /dev/null; then
    echo "ERROR: claude CLI를 찾을 수 없습니다."
    echo "설치: https://docs.anthropic.com/en/docs/claude-code"
    exit 1
fi

# ─── 초기화 ─────────────────────────────────────────────────────────

mkdir -p "$LOG_DIR"
echo $$ > "$LOCKFILE"
trap cleanup EXIT

completed=()
fail_spec=""
fail_reason=""
iteration=0

log "impl-batch 시작 (max=$MAX_ITERATIONS)"
log "프로젝트: $PROJECT_ROOT"
log "로그: $LOG_DIR"

# ─── 메인 루프 ──────────────────────────────────────────────────────

while [ "$iteration" -lt "$MAX_ITERATIONS" ]; do
    iteration=$((iteration + 1))
    iter_log="$LOG_DIR/iter-$iteration-$TIMESTAMP.log"

    log ""
    log "━━━ 반복 $iteration / $MAX_ITERATIONS ━━━"

    # claude -p "/impl-next" 실행
    log "claude -p \"/impl-next\" 실행 중..."

    set +e
    output=$(cd "$PROJECT_ROOT" && claude -p "/impl-next" --dangerously-skip-permissions 2>&1)
    exit_code=$?
    set -e

    # 반복별 로그 저장
    echo "$output" > "$iter_log"
    log "Claude 종료 코드: $exit_code (로그: $iter_log)"

    # ─── 상태 마커 파싱 (macOS 호환: sed 사용) ───

    # SUCCESS 체크
    success_spec=$(echo "$output" | sed -n 's/.*\[IMPL-BATCH:SUCCESS:\([^]]*\)\].*/\1/p' | tail -1)
    if [ -n "$success_spec" ]; then
        log "SUCCESS: $success_spec"
        completed+=("$success_spec")
        continue
    fi

    # NO_SPECS 체크
    if echo "$output" | grep -q '\[IMPL-BATCH:NO_SPECS\]'; then
        log "NO_SPECS: 구현할 승인 스펙이 없습니다."
        break
    fi

    # FAILED 체크
    failed_line=$(echo "$output" | sed -n 's/.*\[IMPL-BATCH:FAILED:\([^]]*\)\].*/\1/p' | tail -1)
    if [ -n "$failed_line" ]; then
        # spec-id:REASON 형식 파싱
        fail_spec=$(echo "$failed_line" | sed 's/:\([^:]*\)$//')
        fail_reason=$(echo "$failed_line" | sed 's/.*:\([^:]*\)$/\1/')
        log "FAILED: $fail_spec ($fail_reason)"
        log "상세 로그: $iter_log"
        break
    fi

    # 마커 없음 (비정상 종료)
    log "WARNING: 상태 마커를 찾을 수 없습니다. (exit_code=$exit_code)"
    log "상세 로그: $iter_log"
    fail_reason="UNKNOWN (no status marker, exit_code=$exit_code)"
    break

done

# ─── 배치 요약 ──────────────────────────────────────────────────────

print_summary | tee -a "$BATCH_LOG"

# 종료 코드
if [ ${#completed[@]} -gt 0 ] && [ -z "$fail_reason" ]; then
    exit 0
elif [ ${#completed[@]} -gt 0 ] && [ -n "$fail_reason" ]; then
    # 일부 성공 후 실패
    exit 1
elif [ -z "$fail_reason" ]; then
    # NO_SPECS (구현할 것 없음)
    exit 0
else
    # 첫 반복부터 실패
    exit 1
fi
