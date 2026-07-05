#!/usr/bin/env bash
#
# 교육 첨부파일을 레거시 S3(ottogi-hdrive) → 신규 시스템 S3 버킷으로 이관하는 일회성 스크립트.
#
# 배경:
#   - 신규 DB education_post_attachment.file_key 는 이미 레거시 값(예: 161733...jpg)으로 마이그레이션됨.
#   - 신규 백엔드 FileStorageService.getEducationFileUrl(fileKey)
#     → StorageService.getPresignedUrl("education/"+fileKey)
#     → 실제 S3 key = "private/education/<fileKey>" 를 presigned URL 로 발급한다.
#     (교육 첨부는 권한 통제 대상이라 private/ 세그먼트 아래에 저장. DB file_key 는 segment 없는 원본값 유지)
#   - 따라서 실제 파일을 "신규 버킷의 private/education/<file_key>" 에 올리면 코드 수정 없이
#     모바일 교육 페이지에서 조회된다.
#
# 이관 대상: education_post_attachment_file_key.csv 의 file_key 만.
#
# 두 가지 모드:
#   MODE=copy   (기본) : 레거시 ottogi-hdrive → 신규 버킷 서버사이드 복사 (로컬 경유 X, 빠름)
#   MODE=upload         : 로컬 downloads/ 폴더의 파일을 신규 버킷으로 업로드 (이미 받아둔 파일 재활용)
#
# 사용법:
#   S3_BUCKET=<신규버킷명> ./migrate_to_new_bucket.sh            # copy 모드
#   S3_BUCKET=<신규버킷명> MODE=upload ./migrate_to_new_bucket.sh # upload 모드
#

set -uo pipefail

# ---- 설정 ----------------------------------------------------------------
SRC_BUCKET="ottogi-hdrive"          # 레거시 (파일 실물 위치)
REGION="ap-northeast-2"
MODE="${MODE:-copy}"                 # copy | upload

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CSV_FILE="${SCRIPT_DIR}/education_post_attachment_file_key.csv"
LOCAL_DIR="${SCRIPT_DIR}/downloads"

# 신규 버킷 내 교육 첨부 저장 prefix. 백엔드가 private/education/<fileKey> 를 presign 하므로 고정.
DST_PREFIX="private/education/"

# 신규 버킷: env S3_BUCKET 우선, 없으면 프롬프트
DST_BUCKET="${S3_BUCKET:-}"

# ---- 사전 점검 ------------------------------------------------------------
command -v aws >/dev/null 2>&1 || { echo "[ERROR] aws CLI 미설치" >&2; exit 1; }
[[ -f "${CSV_FILE}" ]] || { echo "[ERROR] CSV 없음: ${CSV_FILE}" >&2; exit 1; }

if [[ -z "${DST_BUCKET}" ]]; then
  read -r -p "신규 시스템 S3 버킷명 (S3_BUCKET): " DST_BUCKET
fi
[[ -n "${DST_BUCKET}" ]] || { echo "[ERROR] 신규 버킷명 필수" >&2; exit 1; }

if [[ "${MODE}" == "upload" ]]; then
  [[ -d "${LOCAL_DIR}" ]] || { echo "[ERROR] 로컬 downloads/ 없음. 먼저 download_attachments.sh 실행" >&2; exit 1; }
fi

# ---- 자격 증명 입력 (레거시+신규 버킷 모두 접근 가능한 키 권장) --------------
read -r -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -r -s -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
echo
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="${REGION}"

echo
echo "모드: ${MODE}"
echo "출발: ${MODE/copy/s3://${SRC_BUCKET}}"; [[ "${MODE}" == "upload" ]] && echo "출발: ${LOCAL_DIR}"
echo "도착: s3://${DST_BUCKET}/${DST_PREFIX}<file_key>"
echo "----------------------------------------"

total=0; ok=0; fail=0
declare -a FAILED_KEYS=()

# S3 object 의 content-type 을 확장자로 추정 (upload 모드용).
guess_content_type() {
  case "${1##*.}" in
    jpg|jpeg|JPG|JPEG) echo "image/jpeg" ;;
    png|PNG)           echo "image/png" ;;
    gif|GIF)           echo "image/gif" ;;
    mp4|MP4)           echo "video/mp4" ;;
    mov|MOV)           echo "video/quicktime" ;;
    pdf|PDF)           echo "application/pdf" ;;
    url)               echo "text/plain" ;;
    *)                 echo "application/octet-stream" ;;
  esac
}

while IFS= read -r raw_key || [[ -n "${raw_key}" ]]; do
  key="$(printf '%s' "${raw_key}" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
  [[ -z "${key}" || "${key}" == "file_key" ]] && continue

  total=$((total + 1))
  printf '[%3d] %s ... ' "${total}" "${key}"

  rc=0
  if [[ "${MODE}" == "copy" ]]; then
    # 서버사이드 복사: 레거시(루트) → 신규(private/education/). content-type/metadata 는 원본 유지.
    err="$(aws s3 cp "s3://${SRC_BUCKET}/${key}" "s3://${DST_BUCKET}/${DST_PREFIX}${key}" \
             --no-progress 2>&1 >/dev/null)" || rc=$?
  else
    # 로컬 업로드
    src="${LOCAL_DIR}/${key}"
    if [[ ! -f "${src}" ]]; then
      echo "SKIP(로컬 파일 없음)"; fail=$((fail+1)); FAILED_KEYS+=("${key} (로컬 없음)"); continue
    fi
    err="$(aws s3 cp "${src}" "s3://${DST_BUCKET}/${DST_PREFIX}${key}" \
             --content-type "$(guess_content_type "${key}")" \
             --no-progress 2>&1 >/dev/null)" || rc=$?
  fi

  if [[ ${rc} -eq 0 ]]; then
    echo "OK"; ok=$((ok+1))
  else
    echo "FAIL"
    (( fail < 3 )) && { echo "        └─ ${err}" | head -c 400; echo; }
    fail=$((fail+1)); FAILED_KEYS+=("${key}")
  fi
done < "${CSV_FILE}"

echo
echo "========================================"
echo " 대상: ${total}  |  성공: ${ok}  |  실패: ${fail}"
echo " 도착: s3://${DST_BUCKET}/${DST_PREFIX}"
echo "========================================"
if (( fail > 0 )); then
  echo; echo "[실패 목록]"
  for k in "${FAILED_KEYS[@]}"; do echo "  - ${k}"; done
  exit 1
fi
echo
echo "→ 이관 완료. 모바일 교육 페이지에서 첨부가 조회되는지 확인하세요."
echo "  (백엔드/모바일 코드 수정 불필요 — file_key 가 신규 버킷 루트 key 와 일치)"
