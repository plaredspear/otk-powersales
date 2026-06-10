#!/usr/bin/env bash
#
# build-packages.sh — Flutter 모바일 패키지 일괄 빌드 + 버전 자동 증가
#
# pubspec.yaml 의 단일 version(X.Y.Z+N)을 출처로, iOS·Android 가 같은
# versionName / versionCode 를 공유한다(공용 버전 단일 추적).
#
#   - versionCode(+N): 빌드마다 +1 자동 증가
#   - versionName(X.Y.Z): 유지 / patch / minor / major / 직접입력 중 선택
#   - 빌드 범위: --platform {ios|android|all} --env {dev|prod|all}
#   - 버전 증가 후 pubspec.yaml 자동 커밋 + 버전 태그(mobile-v<X.Y.Z>+<N>)
#   - 릴리즈 노트: 직전 버전 태그 이후 mobile/ 커밋에서 초안 출력(확정은 별도)
#
# 사용 예:
#   mobile/scripts/build-packages.sh                      # 대화형(버전·범위 질문)
#   mobile/scripts/build-packages.sh --platform all --env dev --bump patch
#   mobile/scripts/build-packages.sh --platform ios --env prod --bump keep --no-commit
#
set -euo pipefail

# ── 경로 ─────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PUBSPEC="$MOBILE_DIR/pubspec.yaml"
cd "$MOBILE_DIR"

# ── 인자 파싱 ────────────────────────────────────────────────────────
PLATFORM=""        # ios | android | all
ENV=""             # dev | prod | all
BUMP=""            # keep | patch | minor | major | <X.Y.Z>
DO_COMMIT="yes"
DO_TAG="yes"

usage() {
  sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//; /^set -euo/d'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --platform) PLATFORM="${2:-}"; shift 2 ;;
    --env)      ENV="${2:-}"; shift 2 ;;
    --bump)     BUMP="${2:-}"; shift 2 ;;
    --no-commit) DO_COMMIT="no"; shift ;;
    --no-tag)   DO_TAG="no"; shift ;;
    -h|--help)  usage 0 ;;
    *) echo "알 수 없는 인자: $1" >&2; usage 1 ;;
  esac
done

# ── 유틸 ─────────────────────────────────────────────────────────────
err()  { echo "❌ $*" >&2; exit 1; }
info() { echo "▶ $*"; }
ok()   { echo "✅ $*"; }

ask() { # prompt default → 응답을 stdout
  local prompt="$1" default="${2:-}" reply
  if [[ -n "$default" ]]; then
    read -r -p "$prompt [$default]: " reply
    echo "${reply:-$default}"
  else
    read -r -p "$prompt: " reply
    echo "$reply"
  fi
}

# ── 현재 버전 파싱 ───────────────────────────────────────────────────
# pubspec: "version: X.Y.Z+N"
CURRENT_LINE="$(grep -E '^version:' "$PUBSPEC" | head -1)"
[[ -n "$CURRENT_LINE" ]] || err "pubspec.yaml 에서 version 을 찾지 못했습니다"
CURRENT_VERSION="$(echo "$CURRENT_LINE" | sed -E 's/^version:[[:space:]]*//')"
CURRENT_NAME="${CURRENT_VERSION%%+*}"   # X.Y.Z
CURRENT_CODE="${CURRENT_VERSION##*+}"   # N
[[ "$CURRENT_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || err "versionName 형식이 X.Y.Z 가 아닙니다: $CURRENT_NAME"
[[ "$CURRENT_CODE" =~ ^[0-9]+$ ]] || err "versionCode 가 정수가 아닙니다: $CURRENT_CODE"

info "현재 버전: $CURRENT_NAME+$CURRENT_CODE"

# ── 빌드 범위 결정(미지정 시 질문) ──────────────────────────────────
if [[ -z "$PLATFORM" ]]; then
  PLATFORM="$(ask 'platform (ios/android/all)' 'all')"
fi
if [[ -z "$ENV" ]]; then
  ENV="$(ask 'env (dev/prod/all)' 'dev')"
fi
case "$PLATFORM" in ios|android|all) ;; *) err "platform 은 ios|android|all 중 하나" ;; esac
case "$ENV" in dev|prod|all) ;; *) err "env 는 dev|prod|all 중 하나" ;; esac

# ── versionName 증가 규칙 결정 ──────────────────────────────────────
if [[ -z "$BUMP" ]]; then
  echo ""
  echo "versionName 변경 방식:"
  echo "  keep   = 유지 ($CURRENT_NAME)"
  echo "  patch  = ${CURRENT_NAME%.*}.$(( ${CURRENT_NAME##*.} + 1 ))"
  IFS='.' read -r MA MI PA <<< "$CURRENT_NAME"
  echo "  minor  = $MA.$(( MI + 1 )).0"
  echo "  major  = $(( MA + 1 )).0.0"
  echo "  또는 X.Y.Z 직접 입력"
  BUMP="$(ask 'bump (keep/patch/minor/major/X.Y.Z)' 'keep')"
fi

IFS='.' read -r MA MI PA <<< "$CURRENT_NAME"
case "$BUMP" in
  keep)  NEW_NAME="$CURRENT_NAME" ;;
  patch) NEW_NAME="$MA.$MI.$(( PA + 1 ))" ;;
  minor) NEW_NAME="$MA.$(( MI + 1 )).0" ;;
  major) NEW_NAME="$(( MA + 1 )).0.0" ;;
  *)
    [[ "$BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || err "bump 값이 keep|patch|minor|major|X.Y.Z 형식이 아닙니다: $BUMP"
    NEW_NAME="$BUMP" ;;
esac

# versionCode 는 항상 +1
NEW_CODE="$(( CURRENT_CODE + 1 ))"
NEW_VERSION="$NEW_NAME+$NEW_CODE"

echo ""
info "새 버전: $NEW_VERSION  (versionName $CURRENT_NAME → $NEW_NAME, versionCode $CURRENT_CODE → $NEW_CODE)"
info "빌드 대상: platform=$PLATFORM, env=$ENV"

# ── 릴리즈 노트 초안 (직전 버전 태그 이후 mobile/ 커밋) ──────────────
LAST_TAG="$(git tag --list 'mobile-v*' --sort=-creatordate | head -1 || true)"
echo ""
echo "── 릴리즈 노트 초안 (mobile/ 변경) ──"
if [[ -n "$LAST_TAG" ]]; then
  echo "  범위: $LAST_TAG..HEAD"
  git log --oneline "$LAST_TAG..HEAD" -- mobile/ 2>/dev/null | sed 's/^/  - /' || echo "  (없음)"
else
  echo "  (이전 버전 태그 없음 — 최근 mobile/ 커밋 10건)"
  git log --oneline -10 -- mobile/ 2>/dev/null | sed 's/^/  - /' || echo "  (없음)"
fi
echo "  ※ 릴리즈 노트 최종 확정은 별도 — 업로드 시 releaseNote 로 입력"
echo ""

# ── 확인 ─────────────────────────────────────────────────────────────
CONFIRM="$(ask '위 내용으로 진행할까요? (y/n)' 'y')"
[[ "$CONFIRM" == "y" || "$CONFIRM" == "Y" ]] || err "취소했습니다"

# ── pubspec 버전 갱신 ────────────────────────────────────────────────
# macOS/BSD sed 호환: 임시 파일 사용
sed -E "s/^version:[[:space:]]*.*/version: $NEW_VERSION/" "$PUBSPEC" > "$PUBSPEC.tmp"
mv "$PUBSPEC.tmp" "$PUBSPEC"
ok "pubspec.yaml → version: $NEW_VERSION"

# ── 빌드 실행 ────────────────────────────────────────────────────────
build_one() { # <make-target>
  local target="$1"
  info "make $target"
  make "$target"
}

ENVS=()
case "$ENV" in dev) ENVS=(dev) ;; prod) ENVS=(prod) ;; all) ENVS=(dev prod) ;; esac

BUILT=()
for e in "${ENVS[@]}"; do
  if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "all" ]]; then
    build_one "build-ipa-$e"; BUILT+=("ipa-$e")
  fi
  if [[ "$PLATFORM" == "android" || "$PLATFORM" == "all" ]]; then
    build_one "build-apk-$e"; BUILT+=("apk-$e")
  fi
done

# ── 산출물 경로 출력 ─────────────────────────────────────────────────
echo ""
ok "빌드 완료: ${BUILT[*]}"
echo "── 산출물 ──"
[[ -d build/ios/ipa ]] && ls -la build/ios/ipa/*.ipa 2>/dev/null | sed 's/^/  /' || true
[[ -d build/app/outputs/flutter-apk ]] && ls -la build/app/outputs/flutter-apk/*.apk 2>/dev/null | sed 's/^/  /' || true

# ── 커밋 + 태그 ──────────────────────────────────────────────────────
if [[ "$DO_COMMIT" == "yes" ]]; then
  git add "$PUBSPEC"
  git commit -m "chore(mobile): bump version to $NEW_VERSION" >/dev/null
  ok "커밋: chore(mobile): bump version to $NEW_VERSION"
  if [[ "$DO_TAG" == "yes" ]]; then
    TAG="mobile-v$NEW_VERSION"
    git tag "$TAG"
    ok "태그: $TAG"
  fi
else
  echo "ℹ️  --no-commit: pubspec.yaml 변경은 커밋하지 않았습니다(수동 커밋 필요)"
fi

echo ""
ok "전체 완료 — $NEW_VERSION (${BUILT[*]})"
