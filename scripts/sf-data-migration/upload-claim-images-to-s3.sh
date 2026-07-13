#!/usr/bin/env bash
#
# 클레임 이미지 + 메타 CSV S3 업로드 (migrate-claim-images.sh 산출물 → 신규 storage 버킷)
#
# 배경:
#   migrate-claim-images.sh 가 로컬에 준비한 산출물(input/claim-images/)을 신규 시스템
#   S3 버킷으로 업로드한다. 클레임 이미지는 권한 통제 대상이라 private/ 하위 + presigned
#   URL 조회로만 노출되므로, 이미지는 private/uploads/claim/migrated/ 로 올린다.
#
#   업로드 대상 두 곳:
#     ① 이미지  input/claim-images/images/*        → s3://<bucket>/private/uploads/claim/migrated/
#                (실 객체 key = private/ + upload_file.unique_key. DB unique_key 는 segment 미포함)
#     ② 메타 CSV input/claim-images/claim_upload_files.csv
#                                                   → s3://<bucket>/<stage1-prefix>/claim_upload_files.csv
#                (web SF Migration Stage 1 '클레임 이미지' 카드가 읽는 위치. 기본 prefix = sf-migration/claim-images)
#
# 멱등: aws s3 cp 는 재실행 시 동일 객체를 덮어쓰므로 안전. 이미지 파일명
#   ({ContentVersion.Id}.{ext}) 을 바꾸지 말 것 — CSV UniqueKey__c 와 1:1.
#
# 사용법:
#   ./upload-claim-images-to-s3.sh --bucket prod-otk-pwrs-storage
#       → 이미지 + CSV 업로드 (확인 프롬프트 있음)
#   ./upload-claim-images-to-s3.sh --bucket prod-otk-pwrs-storage --dry-run
#       → 무엇이 업로드될지만 확인 (실제 업로드 안 함)
#   ./upload-claim-images-to-s3.sh --bucket prod-otk-pwrs-storage --yes
#       → 확인 프롬프트 생략 (자동화)
#   ./upload-claim-images-to-s3.sh --bucket prod-otk-pwrs-storage --skip-images    # CSV 만
#   ./upload-claim-images-to-s3.sh --bucket prod-otk-pwrs-storage --skip-csv       # 이미지만
#
# 사전:
#   - migrate-claim-images.sh 로 input/claim-images/ 산출물 준비 완료.
#   - 해당 버킷에 s3:PutObject 가능한 AWS 자격증명(SSO/프로파일) 설정.
#   - aws CLI 설치 (aws --version).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# -----------------------------------------------------------------------------
# 기본값 / 인자 파싱
# -----------------------------------------------------------------------------

BUCKET=""
# AWS CLI 프로파일 (선택). 미지정 시 aws 기본 자격증명(AWS_PROFILE 환경변수 등) 사용.
AWS_PROFILE_NAME=""
# 로컬 산출물 위치 — migrate-claim-images.sh 의 OUT_DIR 과 동일.
OUT_DIR="$SCRIPT_DIR/input/claim-images"
# 이미지 S3 prefix — 실 객체 key = private/ + unique_key. migrate-claim-images.sh 의
# PRIVATE_SEGMENT("private") + IMAGE_PREFIX("uploads/claim/migrated") 와 정합.
IMAGE_S3_PREFIX="private/uploads/claim/migrated"
# CSV S3 prefix — web Stage 1 이 읽는 위치. migrate-claim-images.sh 의 STAGE1_PREFIX 기본값과 동일.
STAGE1_PREFIX="sf-migration/claim-images"
CSV_NAME="claim_upload_files.csv"

DRY_RUN=false
ASSUME_YES=false
SKIP_IMAGES=false
SKIP_CSV=false

usage() {
	cat <<'USAGE'
사용법: upload-claim-images-to-s3.sh --bucket <S3_BUCKET> [옵션]

  --bucket <name>       대상 S3 버킷 (필수). backend app.aws.s3.bucket 와 동일.
  --profile <name>      AWS CLI 프로파일 (선택, 예: prod-otk-pwrs-db-access).
  --out-dir <path>      로컬 산출물 디렉토리 (기본 input/claim-images).
  --stage1-prefix <p>   CSV 업로드 prefix (기본 sf-migration/claim-images).
  --dry-run             실제 업로드 없이 대상만 표시.
  --skip-images         이미지 업로드 건너뜀 (CSV 만).
  --skip-csv            CSV 업로드 건너뜀 (이미지만).
  -y, --yes             확인 프롬프트 생략.
  -h, --help            이 도움말.
USAGE
}

while [[ $# -gt 0 ]]; do
	case "$1" in
		--bucket)        BUCKET="${2:-}"; shift 2 ;;
		--profile)       AWS_PROFILE_NAME="${2:-}"; shift 2 ;;
		--out-dir)       OUT_DIR="${2:-}"; shift 2 ;;
		--stage1-prefix) STAGE1_PREFIX="${2%/}"; shift 2 ;;
		--dry-run)       DRY_RUN=true; shift ;;
		--skip-images)   SKIP_IMAGES=true; shift ;;
		--skip-csv)      SKIP_CSV=true; shift ;;
		-y|--yes)        ASSUME_YES=true; shift ;;
		-h|--help)       usage; exit 0 ;;
		*) echo "알 수 없는 인자: $1" >&2; usage >&2; exit 1 ;;
	esac
done

# -----------------------------------------------------------------------------
# 검증
# -----------------------------------------------------------------------------

if [[ -z "$BUCKET" ]]; then
	echo "[error] --bucket 은 필수입니다 (예: --bucket prod-otk-pwrs-storage)." >&2
	usage >&2
	exit 1
fi

if ! command -v aws &>/dev/null; then
	echo "[error] aws CLI 가 설치되어 있지 않습니다 (aws --version)." >&2
	exit 1
fi

# aws 공통 인자 — 프로파일 지정 시 모든 호출에 부착.
AWS_ARGS=()
if [[ -n "$AWS_PROFILE_NAME" ]]; then
	AWS_ARGS+=(--profile "$AWS_PROFILE_NAME")
fi

# 자격증명 사전 확인 — 잘못된 프로파일/만료 시 업로드 전에 중단.
if ! aws sts get-caller-identity "${AWS_ARGS[@]}" &>/dev/null; then
	echo "[error] AWS 자격증명 확인 실패${AWS_PROFILE_NAME:+ (profile=$AWS_PROFILE_NAME)}." >&2
	echo "        aws sts get-caller-identity ${AWS_ARGS[*]} 로 점검하세요 (SSO 로그인/프로파일 확인)." >&2
	exit 1
fi

IMG_DIR="$OUT_DIR/images"
CSV_PATH="$OUT_DIR/$CSV_NAME"

if [[ "$SKIP_IMAGES" == false && ! -d "$IMG_DIR" ]]; then
	echo "[error] 이미지 디렉토리 없음: $IMG_DIR" >&2
	echo "        migrate-claim-images.sh 로 먼저 산출물을 준비하세요." >&2
	exit 1
fi

if [[ "$SKIP_CSV" == false && ! -f "$CSV_PATH" ]]; then
	echo "[error] CSV 없음: $CSV_PATH" >&2
	echo "        migrate-claim-images.sh 로 먼저 산출물을 준비하세요." >&2
	exit 1
fi

IMAGE_DEST="s3://$BUCKET/$IMAGE_S3_PREFIX/"
CSV_DEST="s3://$BUCKET/$STAGE1_PREFIX/$CSV_NAME"

# -----------------------------------------------------------------------------
# 사전 정보 출력
# -----------------------------------------------------------------------------

img_count=0
if [[ "$SKIP_IMAGES" == false ]]; then
	img_count=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
fi

echo "============================================================================"
echo " 클레임 이미지 + CSV S3 업로드"
echo "============================================================================"
echo " 버킷        : $BUCKET"
echo " 프로파일    : ${AWS_PROFILE_NAME:-(기본 자격증명)}"
if [[ "$SKIP_IMAGES" == false ]]; then
	echo " 이미지      : $IMG_DIR ($img_count 개) → $IMAGE_DEST"
else
	echo " 이미지      : (건너뜀)"
fi
if [[ "$SKIP_CSV" == false ]]; then
	echo " CSV         : $CSV_PATH → $CSV_DEST"
else
	echo " CSV         : (건너뜀)"
fi
echo " 모드        : $([[ "$DRY_RUN" == true ]] && echo 'DRY-RUN (실제 업로드 없음)' || echo '실제 업로드')"
echo "----------------------------------------------------------------------------"

# -----------------------------------------------------------------------------
# 확인 프롬프트
# -----------------------------------------------------------------------------

if [[ "$DRY_RUN" == false && "$ASSUME_YES" == false ]]; then
	read -r -p "위 대상으로 업로드하시겠습니까? [y/N] " answer
	if [[ ! "$answer" =~ ^[Yy]$ ]]; then
		echo "[cancel] 사용자에 의해 취소되었습니다."
		exit 0
	fi
fi

CP_OPTS=()
if [[ "$DRY_RUN" == true ]]; then
	CP_OPTS+=(--dryrun)
fi

# -----------------------------------------------------------------------------
# ① 이미지 업로드
# -----------------------------------------------------------------------------

if [[ "$SKIP_IMAGES" == false ]]; then
	echo
	echo "[1/2] 이미지 업로드 → $IMAGE_DEST"
	aws s3 cp "$IMG_DIR/" "$IMAGE_DEST" --recursive "${AWS_ARGS[@]}" "${CP_OPTS[@]}"
	echo "[ok] 이미지 업로드 완료"
fi

# -----------------------------------------------------------------------------
# ② CSV 업로드
# -----------------------------------------------------------------------------

if [[ "$SKIP_CSV" == false ]]; then
	echo
	echo "[2/2] CSV 업로드 → $CSV_DEST"
	aws s3 cp "$CSV_PATH" "$CSV_DEST" "${AWS_ARGS[@]}" "${CP_OPTS[@]}"
	echo "[ok] CSV 업로드 완료"
fi

# -----------------------------------------------------------------------------
# 검증 안내
# -----------------------------------------------------------------------------

echo
if [[ "$DRY_RUN" == true ]]; then
	echo "[done] DRY-RUN 종료 — 실제 업로드는 --dry-run 없이 재실행."
	exit 0
fi

PROFILE_HINT="${AWS_PROFILE_NAME:+ --profile $AWS_PROFILE_NAME}"
echo "============================================================================"
echo " 업로드 완료. 검증:"
if [[ "$SKIP_IMAGES" == false ]]; then
	echo "   aws s3 ls $IMAGE_DEST --recursive$PROFILE_HINT | wc -l    # 기대: $img_count"
fi
if [[ "$SKIP_CSV" == false ]]; then
	echo "   aws s3 ls $CSV_DEST$PROFILE_HINT"
fi
echo
echo " 다음 단계 (web SF Migration):"
echo "   1) Stage 1 '클레임 이미지' 카드 적재 (target=ClaimImageUploadFile, s3KeyPrefix=$STAGE1_PREFIX)"
echo "   2) Stage 2 upload-file-polymorphic-parent substep"
echo "============================================================================"
