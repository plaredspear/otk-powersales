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
#   mobile/scripts/build-packages.sh --platform ios --env prod --bump patch --no-commit
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
BUMP=""            # patch | minor | major | <X.Y.Z>
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
  echo "  patch  = ${CURRENT_NAME%.*}.$(( ${CURRENT_NAME##*.} + 1 ))"
  IFS='.' read -r MA MI PA <<< "$CURRENT_NAME"
  echo "  minor  = $MA.$(( MI + 1 )).0"
  echo "  major  = $(( MA + 1 )).0.0"
  echo "  또는 X.Y.Z 직접 입력"
  BUMP="$(ask 'bump (patch/minor/major/X.Y.Z)' 'patch')"
fi

IFS='.' read -r MA MI PA <<< "$CURRENT_NAME"
case "$BUMP" in
  patch) NEW_NAME="$MA.$MI.$(( PA + 1 ))" ;;
  minor) NEW_NAME="$MA.$(( MI + 1 )).0" ;;
  major) NEW_NAME="$(( MA + 1 )).0.0" ;;
  *)
    [[ "$BUMP" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || err "bump 값이 patch|minor|major|X.Y.Z 형식이 아닙니다: $BUMP"
    NEW_NAME="$BUMP" ;;
esac

# versionCode 는 항상 +1
NEW_CODE="$(( CURRENT_CODE + 1 ))"
NEW_VERSION="$NEW_NAME+$NEW_CODE"

echo ""
info "새 버전: $NEW_VERSION  (versionName $CURRENT_NAME → $NEW_NAME, versionCode $CURRENT_CODE → $NEW_CODE)"
info "빌드 대상: platform=$PLATFORM, env=$ENV"

# ── 릴리즈 노트 (feat/fix 만 — 사용자=영업사원 관점) ─────────────────
# 규칙:
#   - 포함 타입: feat, fix 만 (build/chore/refactor/style/test 는 내부 변경이라 제외)
#   - 최종 문구는 사용자 친화 표현으로 다듬어야 함(AI 번역). 스크립트는 원커밋
#     기반 골격만 생성하고, /build-packages 커맨드가 이를 다듬어 확정한다.
#   - 저장: mobile/release-notes/<X.Y.Z+N>.md (git 커밋), 업로드 시 releaseNote 로 사용.
RELEASE_NOTES_DIR="$MOBILE_DIR/release-notes"
RELEASE_NOTES_FILE="$RELEASE_NOTES_DIR/$NEW_VERSION.md"
LAST_TAG="$(git tag --list 'mobile-v*' --sort=-creatordate | head -1 || true)"
if [[ -n "$LAST_TAG" ]]; then RANGE="$LAST_TAG..HEAD"; else RANGE=""; fi

echo ""
if [[ -f "$RELEASE_NOTES_FILE" ]]; then
  # /build-packages 커맨드가 미리 작성·확정한 릴리즈 노트가 있으면 그대로 사용.
  echo "── 릴리즈 노트 (확정본 사용) ── $RELEASE_NOTES_FILE"
  sed 's/^/  /' "$RELEASE_NOTES_FILE"
else
  # 골격 자동 생성: feat/fix 만 추출. 문구 다듬기는 별도(커맨드).
  echo "── 릴리즈 노트 초안 (feat/fix, 골격) ──"
  if [[ -n "$RANGE" ]]; then echo "  범위: $RANGE"; else echo "  (이전 버전 태그 없음 — 전체 mobile/ 이력)"; fi
  # cwd 가 mobile/ 이므로 pathspec 은 '.' (현재 디렉토리 = mobile/)
  FEATFIX="$(git log --pretty=format:'%s' ${RANGE} -- . 2>/dev/null \
    | grep -E '^(feat|fix)(\(.*\))?(!)?:' || true)"
  if [[ -z "$FEATFIX" ]]; then
    echo "  (feat/fix 커밋 없음)"
  else
    echo "$FEATFIX" | sed -E 's/^(feat|fix)(\(.*\))?(!)?:[[:space:]]*/  - /'
  fi
  echo "  ※ 위는 원커밋 골격. 사용자 친화 문구로 다듬어 다음 파일에 저장 권장:"
  echo "     $RELEASE_NOTES_FILE  (업로드 시 releaseNote 로 사용)"
fi
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

# ── 기존 빌드 산출물 정리 (stale 산출물 방지) ───────────────────────
# 이전 빌드의 APK/IPA 와 빌드 캐시(.dart_tool, build/)가 남아 있으면 (1) Flutter/
# Gradle 의 incremental 빌드가 산출물 갱신을 건너뛰어 구 소스 기반 산출물이 그대로
# 남거나, (2) 빌드가 갱신에 실패해도 디렉토리의 구 APK 가 그대로 복사되어, 파일명은
# 신버전인데 내용은 구버전인 패키지가 배포될 수 있다. 매 빌드 전 clean 으로
# .dart_tool + build/ 를 통째로 지워 항상 현재 소스 기준으로 새로 빌드한다.
# (make clean = flutter clean + rm -rf .dart_tool build)
info "기존 빌드 산출물 정리 (make clean — 이전 APK/IPA + 캐시 삭제)"
make clean

# 산출물 공통 폴더. 빌드는 Flutter 기본 위치에 생성되고(원본 보존), 빌드 직후
# build/dist 로 복사하며 <base>-<flavor>-<version> 으로 파일명을 정규화한다.
# IPA 는 flavor 무관하게 build/ios/ipa/mobile.ipa 한 이름으로 덮여 생성되므로,
# 다음 flavor 빌드가 덮어쓰기 전에 flavor 별 빌드 직후 즉시 복사해야 한다.
# 위 make clean 이 build/ 를 통째로 지우므로 여기서 다시 생성한다.
DIST_DIR="$MOBILE_DIR/build/dist"
mkdir -p "$DIST_DIR"

BUILT=()
for e in "${ENVS[@]}"; do
  if [[ "$PLATFORM" == "ios" || "$PLATFORM" == "all" ]]; then
    build_one "build-ipa-$e"; BUILT+=("ipa-$e")
    src="build/ios/ipa/mobile.ipa"
    [[ -f "$src" ]] && cp -f "$src" "$DIST_DIR/mobile-$e-$NEW_VERSION.ipa"
  fi
  if [[ "$PLATFORM" == "android" || "$PLATFORM" == "all" ]]; then
    build_one "build-apk-$e"; BUILT+=("apk-$e")
    src="build/app/outputs/flutter-apk/app-$e-release.apk"
    [[ -f "$src" ]] && cp -f "$src" "$DIST_DIR/app-$e-$NEW_VERSION.apk"
  fi
done

# ── 산출물 경로 출력 ─────────────────────────────────────────────────
echo ""
ok "빌드 완료: ${BUILT[*]}"
echo "── 산출물 (build/dist) ──"
ls -la "$DIST_DIR"/*-"$NEW_VERSION".{ipa,apk} 2>/dev/null | sed 's/^/  /' || true

# ── 커밋 + 태그 ──────────────────────────────────────────────────────
if [[ "$DO_COMMIT" == "yes" ]]; then
  git add "$PUBSPEC"
  COMMIT_SUBJECT="chore(mobile): bump version to $NEW_VERSION"
  # 릴리즈 노트 확정본이 있으면 함께 커밋하고, 그 내용을 축약해 커밋 본문에 포함한다.
  COMMIT_BODY=""
  if [[ -f "$RELEASE_NOTES_FILE" ]]; then
    git add "$RELEASE_NOTES_FILE"
    # 마크다운을 커밋 본문용으로 축약:
    #   - '# 제목' 라인 제외(버전은 subject 에 이미 있음)
    #   - '## 섹션' → '[섹션]' 라벨, 섹션별 항목은 최대 4개 + '- 외 N건'
    #   - 그 외 '- 항목' 은 유지, 빈 줄/기타 텍스트는 제외
    COMMIT_BODY="$(awk '
      function flush(  i) {
        if (sec != "") {
          printf "\n[%s]\n", sec
          for (i = 1; i <= n && i <= 4; i++) print items[i]
          if (n > 4) printf "- 외 %d건\n", n - 4
        }
      }
      /^#[^#]/     { next }
      /^##[ \t]/   { flush(); sub(/^##[ \t]*/, ""); sec=$0; n=0; next }
      /^[-*][ \t]/ { sub(/^[-*][ \t]*/, "- "); n++; items[n]=$0; next }
                   { next }
      END          { flush() }
    ' "$RELEASE_NOTES_FILE")"
  fi
  if [[ -n "$COMMIT_BODY" ]]; then
    git commit -m "$COMMIT_SUBJECT" -m "$COMMIT_BODY" >/dev/null
  else
    git commit -m "$COMMIT_SUBJECT" >/dev/null
  fi
  ok "커밋: $COMMIT_SUBJECT"
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
