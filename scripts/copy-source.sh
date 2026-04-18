#!/usr/bin/env bash
#
# copy-source.sh
#
# backend/, mobile/, web/ 폴더를 지정된 대상 경로로 복사(미러)한다.
# rsync --delete 로 대상의 해당 폴더를 원본과 완전히 일치시킨다.
# 대상이 git repo 인 경우 --commit, --push 옵션으로 자동 커밋/푸시 가능.
#
# Usage:
#   ./scripts/copy-source.sh <target-dir>
#   ./scripts/copy-source.sh <target-dir> --commit
#   ./scripts/copy-source.sh <target-dir> --commit --push
#   ./scripts/copy-source.sh <target-dir> --dry-run
#
# Examples:
#   ./scripts/copy-source.sh /tmp/otoki-backup
#   ./scripts/copy-source.sh ~/dev/otoki-mirror --commit --push
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

FOLDERS=(backend mobile web)

################################################################################
# 유틸리티
################################################################################

log()  { echo "$(date '+%H:%M:%S') [INFO]  $*"; }
warn() { echo "$(date '+%H:%M:%S') [WARN]  $*" >&2; }
err()  { echo "$(date '+%H:%M:%S') [ERROR] $*" >&2; }

usage() {
  sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//;s/^set -euo pipefail$//'
  exit 1
}

################################################################################
# 인자 파싱
################################################################################

TARGET=""
DO_COMMIT=false
DO_PUSH=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --commit)  DO_COMMIT=true ;;
    --push)    DO_PUSH=true ;;
    --dry-run) DRY_RUN=true ;;
    -h|--help) usage ;;
    -*) err "알 수 없는 옵션: $1"; usage ;;
    *)
      if [[ -z "$TARGET" ]]; then
        TARGET="$1"
      else
        err "대상 경로는 1개만 지정 가능: '$TARGET' vs '$1'"
        exit 1
      fi
      ;;
  esac
  shift
done

[[ -z "$TARGET" ]] && { err "대상 경로가 필요합니다"; usage; }
$DO_PUSH && ! $DO_COMMIT && { err "--push 는 --commit 과 함께 사용해야 합니다"; exit 1; }

################################################################################
# 원본 검증
################################################################################

for f in "${FOLDERS[@]}"; do
  if [[ ! -d "$PROJECT_ROOT/$f" ]]; then
    err "원본 폴더가 없습니다: $PROJECT_ROOT/$f"
    exit 1
  fi
done

################################################################################
# 대상 준비
################################################################################

# 상대 경로 → 절대 경로 (존재하지 않아도 처리)
if [[ "$TARGET" != /* ]]; then
  TARGET="$(cd "$(dirname "$TARGET")" 2>/dev/null && pwd)/$(basename "$TARGET")" || {
    err "대상 경로의 상위 디렉토리가 없습니다: $(dirname "$TARGET")"
    exit 1
  }
fi

if [[ ! -d "$TARGET" ]]; then
  log "대상 디렉토리를 생성합니다: $TARGET"
  $DRY_RUN || mkdir -p "$TARGET"
fi

# 자기 자신 복사 방지
if [[ "$TARGET" == "$PROJECT_ROOT" || "$TARGET" == "$PROJECT_ROOT/"* ]]; then
  err "대상 경로가 프로젝트 내부입니다. 순환 복사 방지를 위해 중단합니다: $TARGET"
  exit 1
fi

################################################################################
# rsync 실행
################################################################################

RSYNC_OPTS=(-a --delete --exclude='.git' --exclude='node_modules' --exclude='build' --exclude='.gradle' --exclude='.idea' --exclude='dist' --exclude='.dart_tool' --exclude='.flutter-plugins*')
$DRY_RUN && RSYNC_OPTS+=(--dry-run --itemize-changes)

log "원본: $PROJECT_ROOT"
log "대상: $TARGET"
$DRY_RUN && log "[DRY RUN] 실제 파일 변경 없음"

for f in "${FOLDERS[@]}"; do
  log "sync: $f/"
  rsync "${RSYNC_OPTS[@]}" "$PROJECT_ROOT/$f/" "$TARGET/$f/"
done

$DRY_RUN && { log "dry-run 완료"; exit 0; }

################################################################################
# git commit / push (--commit 지정 시)
################################################################################

if $DO_COMMIT; then
  if [[ ! -d "$TARGET/.git" ]]; then
    err "대상이 git repo 가 아닙니다 (--commit 불가): $TARGET"
    exit 1
  fi

  SRC_SHA=$(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown")
  SRC_BRANCH=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

  cd "$TARGET"
  if [[ -z "$(git status --porcelain)" ]]; then
    log "변경 사항 없음, commit 생략"
  else
    git add -A "${FOLDERS[@]}"
    git commit -m "mirror: sync from ${SRC_BRANCH}@${SRC_SHA}"
    log "commit 생성 완료"

    if $DO_PUSH; then
      log "원격으로 push 중..."
      git push
      log "push 완료"
    fi
  fi
fi

log "완료"
