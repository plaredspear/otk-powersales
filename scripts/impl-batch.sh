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
#
# Ctrl+C로 즉시 중단 가능

set -uo pipefail

# ─── 설정 ───────────────────────────────────────────────────────────

MAX_ITERATIONS="${1:-50}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
LOG_DIR="$PROJECT_ROOT/docs/execution/impl-batch-logs"
BATCH_LOG="$LOG_DIR/batch-$TIMESTAMP.log"
LOCKFILE="$PROJECT_ROOT/.impl-batch.lock"

# ─── 상태 변수 ────────────────────────────────────────────────────────

INTERRUPTED=false
CLAUDE_PID=""
TAIL_PID=""
completed=()
fail_spec=""
fail_reason=""
iteration=0

# ─── 함수 ───────────────────────────────────────────────────────────

log() {
    local msg="[$(date '+%H:%M:%S')] $1"
    echo "$msg"
    echo "$msg" >> "$BATCH_LOG"
}

cleanup() {
    # 남은 자식 프로세스 강제 종료
    force_kill "$CLAUDE_PID"
    force_kill "$TAIL_PID"
    wait 2>/dev/null || true
    rm -f "$LOCKFILE"
}

force_kill() {
    local pid="$1"
    [ -z "$pid" ] && return
    kill -0 "$pid" 2>/dev/null || return
    kill -TERM "$pid" 2>/dev/null
    # SIGTERM 후 0.5초 대기, 아직 살아있으면 SIGKILL
    local i=0
    while [ $i -lt 5 ] && kill -0 "$pid" 2>/dev/null; do
        sleep 0.1
        i=$((i + 1))
    done
    kill -KILL "$pid" 2>/dev/null || true
}

on_interrupt() {
    INTERRUPTED=true
    echo ""
    log "⚠️  중단 요청됨 (Ctrl+C)"
    # trap 핸들러 안에서는 wait 없이 즉시 kill만 수행
    [ -n "$CLAUDE_PID" ] && kill -KILL "$CLAUDE_PID" 2>/dev/null
    [ -n "$TAIL_PID" ] && kill -KILL "$TAIL_PID" 2>/dev/null
}

print_summary() {
    local end_time
    end_time="$(date +%Y%m%d-%H%M%S)"
    echo ""
    echo "============================================"
    echo "  impl-batch 배치 요약"
    echo "============================================"
    echo "  시작 시각: $TIMESTAMP"
    echo "  종료 시각: $end_time"
    echo "  총 반복:   $iteration / $MAX_ITERATIONS"
    echo "  성공:      ${#completed[@]}"
    if [ ${#completed[@]} -gt 0 ]; then
        echo "  완료 스펙:"
        for spec in "${completed[@]}"; do
            echo "    - $spec"
        done
    fi
    if [ -n "$fail_reason" ]; then
        echo "  종료 사유: $fail_reason"
        if [ -n "$fail_spec" ]; then
            echo "  실패 스펙: $fail_spec"
        fi
    fi
    echo "  로그: $LOG_DIR"
    echo "============================================"
}

# ─── 시그널 트랩 ──────────────────────────────────────────────────────

trap on_interrupt INT TERM
trap cleanup EXIT

# ─── 사전 점검 ──────────────────────────────────────────────────────

# Lockfile 중복 실행 방지
if [ -f "$LOCKFILE" ]; then
    existing_pid=$(cat "$LOCKFILE" 2>/dev/null || echo "unknown")
    if kill -0 "$existing_pid" 2>/dev/null; then
        echo "ERROR: impl-batch가 이미 실행 중입니다 (PID: $existing_pid)"
        echo "강제 종료하려면: rm $LOCKFILE"
        exit 1
    else
        echo "WARN: 이전 실행의 잔여 lockfile 제거 (PID $existing_pid 는 이미 종료됨)"
        rm -f "$LOCKFILE"
    fi
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

log "impl-batch 시작 (max=$MAX_ITERATIONS, PID=$$)"
log "프로젝트: $PROJECT_ROOT"
log "로그: $LOG_DIR"
log "중단: Ctrl+C"

# ─── 메인 루프 ──────────────────────────────────────────────────────

while [ "$iteration" -lt "$MAX_ITERATIONS" ]; do

    # Ctrl+C 체크
    if [ "$INTERRUPTED" = true ]; then
        fail_reason="USER_INTERRUPTED"
        break
    fi

    iteration=$((iteration + 1))
    iter_log="$LOG_DIR/iter-$iteration-$TIMESTAMP.log"
    iter_start="$(date +%s)"

    log ""
    log "━━━ 반복 $iteration / $MAX_ITERATIONS ━━━"
    log "claude 실행 시작..."

    # claude를 백그라운드로 실행 (로그 파일에 출력)
    # → wait가 시그널에 즉시 반응하므로 Ctrl+C가 동작함
    > "$iter_log"
    claude -p "/impl-next" --dangerously-skip-permissions --verbose >> "$iter_log" 2>&1 &
    CLAUDE_PID=$!

    # tail -f로 실시간 터미널 출력
    tail -f "$iter_log" &
    TAIL_PID=$!

    # wait는 시그널 수신 시 즉시 리턴 → trap 핸들러 실행 가능
    wait "$CLAUDE_PID" 2>/dev/null
    exit_code=$?
    CLAUDE_PID=""

    # tail 정리 (SIGKILL로 즉시 종료)
    [ -n "$TAIL_PID" ] && kill -KILL "$TAIL_PID" 2>/dev/null
    wait "$TAIL_PID" 2>/dev/null || true
    TAIL_PID=""

    iter_end="$(date +%s)"
    iter_elapsed=$(( iter_end - iter_start ))
    log "claude 종료 (exit=$exit_code, ${iter_elapsed}초 소요)"

    # Ctrl+C 체크 (claude 실행 중 인터럽트된 경우)
    if [ "$INTERRUPTED" = true ]; then
        fail_reason="USER_INTERRUPTED"
        break
    fi

    # ─── 상태 마커 파싱 (로그 파일에서) ───

    # SUCCESS 체크
    success_spec=$(sed -n 's/.*\[IMPL-BATCH:SUCCESS:\([^]]*\)\].*/\1/p' "$iter_log" | tail -1)
    if [ -n "$success_spec" ]; then
        log "✅ SUCCESS: $success_spec (${iter_elapsed}초)"
        completed+=("$success_spec")
        continue
    fi

    # NO_SPECS 체크
    if grep -q '\[IMPL-BATCH:NO_SPECS\]' "$iter_log"; then
        log "📭 NO_SPECS: 구현할 승인 스펙이 없습니다."
        fail_reason="NO_SPECS"
        break
    fi

    # FAILED 체크
    failed_line=$(sed -n 's/.*\[IMPL-BATCH:FAILED:\([^]]*\)\].*/\1/p' "$iter_log" | tail -1)
    if [ -n "$failed_line" ]; then
        fail_spec=$(echo "$failed_line" | sed 's/:\([^:]*\)$//')
        fail_reason=$(echo "$failed_line" | sed 's/.*:\([^:]*\)$/\1/')
        log "❌ FAILED: $fail_spec ($fail_reason)"
        break
    fi

    # 마커 없음 (비정상 종료)
    log "⚠️  WARNING: 상태 마커를 찾을 수 없습니다 (exit=$exit_code)"
    fail_reason="UNKNOWN (no status marker, exit=$exit_code)"
    break

done

# ─── 배치 요약 ──────────────────────────────────────────────────────

print_summary | tee -a "$BATCH_LOG"

# 종료 코드
if [ "$fail_reason" = "USER_INTERRUPTED" ]; then
    exit 130
elif [ ${#completed[@]} -gt 0 ] && [ -z "$fail_reason" ]; then
    exit 0
elif [ "$fail_reason" = "NO_SPECS" ]; then
    exit 0
elif [ ${#completed[@]} -gt 0 ]; then
    exit 1
else
    exit 1
fi
