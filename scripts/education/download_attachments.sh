#!/usr/bin/env bash
#
# education_post_attachment 테이블의 file_key 에 해당하는 파일을
# S3 (ottogi-real-hdrive) 에서 다운로드하는 일회성 스크립트.
#
# - Access Key / Secret Key 는 실행 시 입력받음 (환경변수 미저장)
# - file_key 목록은 education_post_attachment_file_key.csv 에서 읽음
# - 다운로드 대상은 CSV 에 있는 file_key 파일만
#
# 사용법:
#   ./download_attachments.sh
#

set -euo pipefail

# ---- 설정 ----------------------------------------------------------------
# 버킷명: 실제 교육 첨부 object 는 ottogi-hdrive 에 있다 (레거시 소스 기준 + probe 확인).
# 사용자 최초 제공값 ottogi-real-hdrive 는 빈 버킷이라 전건 NoSuchKey 였음.
BUCKET="ottogi-hdrive"
REGION="ap-northeast-2"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CSV_FILE="${SCRIPT_DIR}/education_post_attachment_file_key.csv"
OUT_DIR="${SCRIPT_DIR}/downloads"

# S3 상에서 file_key 앞에 붙는 prefix.
#
# 레거시(Heroku) 검증 결과: 교육 첨부는 폴더 prefix 없이 버킷 루트에 저장된다.
#   - 업로드: HDriveAWSService.uploadHdriveAWS() → object key = "{timestamp}{empCode}.{ext}" 를
#     PutObjectRequest 의 key 로 그대로 사용 (prefix 부착 없음)
#   - 조회:   community/edu/view.jsp → https://{bucket}.s3.ap-northeast-2.amazonaws.com/{edu_file_key}
#   - edu_file_key 컬럼값 == S3 object key 전체 (신규 EducationPostAttachment.file_key 도 동일값 계승)
# 따라서 file_key 값을 그대로 object key 로 사용 (prefix 없음).
S3_PREFIX=""

# ---- 사전 점검 ------------------------------------------------------------
if ! command -v aws >/dev/null 2>&1; then
  echo "[ERROR] aws CLI 가 설치되어 있지 않습니다." >&2
  exit 1
fi

if [[ ! -f "${CSV_FILE}" ]]; then
  echo "[ERROR] file_key CSV 를 찾을 수 없습니다: ${CSV_FILE}" >&2
  exit 1
fi

# ---- 자격 증명 입력 -------------------------------------------------------
read -r -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -r -s -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
echo
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="${REGION}"

if [[ -z "${AWS_ACCESS_KEY_ID}" || -z "${AWS_SECRET_ACCESS_KEY}" ]]; then
  echo "[ERROR] Access Key / Secret Key 는 필수입니다." >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"

# ---- 다운로드 -------------------------------------------------------------
total=0
ok=0
fail=0
declare -a FAILED_KEYS=()

# CSV 는 첫 줄이 헤더(file_key). 나머지 각 행이 file_key.
while IFS= read -r raw_key || [[ -n "${raw_key}" ]]; do
  # CR 제거 + 앞뒤 공백 제거
  key="$(printf '%s' "${raw_key}" | tr -d '\r' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"

  [[ -z "${key}" ]] && continue
  [[ "${key}" == "file_key" ]] && continue   # 헤더 스킵

  s3_key="${S3_PREFIX}${key}"
  dest="${OUT_DIR}/${key}"

  total=$((total + 1))
  printf '[%3d] s3://%s/%s ... ' "${total}" "${BUCKET}" "${s3_key}"

  rc=0
  err="$(aws s3api get-object \
        --bucket "${BUCKET}" \
        --key "${s3_key}" \
        "${dest}" 2>&1 >/dev/null)" || rc=$?
  if [[ ${rc} -eq 0 ]]; then
    echo "OK"
    ok=$((ok + 1))
  else
    echo "FAIL"
    # 첫 3건까지는 실제 에러 메시지 출력 (원인 진단용)
    if (( fail < 3 )); then
      echo "        └─ ${err}" | head -c 400
      echo
    fi
    fail=$((fail + 1))
    FAILED_KEYS+=("${key}")
    # 0바이트 빈 파일이 생겼으면 정리
    [[ -f "${dest}" && ! -s "${dest}" ]] && rm -f "${dest}"
  fi
done < "${CSV_FILE}"

# ---- 결과 요약 ------------------------------------------------------------
echo
echo "========================================"
echo " 대상: ${total} 건  |  성공: ${ok}  |  실패: ${fail}"
echo " 저장 위치: ${OUT_DIR}"
echo "========================================"

if (( fail > 0 )); then
  echo
  echo "[실패 목록]"
  for k in "${FAILED_KEYS[@]}"; do
    echo "  - ${k}"
  done
  exit 1
fi
