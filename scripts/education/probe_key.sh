#!/usr/bin/env bash
#
# 교육 첨부 file_key 의 실제 위치를 탐색하는 진단 스크립트.
#
# NoSuchKey(404) 원인 후보:
#   ① CSV file_key 가 마스킹된 값 (뒷자리 empCode 가 12345678 로 통일) → key 자체가 실물과 불일치
#   ② 버킷명 불일치 (레거시 소스=ottogi-hdrive / 사용자 제공=ottogi-real-hdrive)
#
# 버킷(2종) × prefix(다수) 매트릭스를 head-object 로 찔러본다.
#   - 어느 조합이든 200(FOUND) → 그 버킷/prefix 확정
#   - 전 조합 404 → ① 마스킹 유력. DB 원본 file_key 재추출 필요
#   - ListBucket 권한이 있으면 실제 몇 개 object key 를 나열해 실물 패턴을 직접 확인
#
# 사용법: ./probe_key.sh [테스트할_file_key]
#

set -uo pipefail

REGION="ap-northeast-2"
TEST_KEY="${1:-161733708713112345678.jpg}"

BUCKETS=(
  "ottogi-real-hdrive"   # 사용자 제공
  "ottogi-hdrive"        # 레거시 소스 기준
)

# 후보 prefix (레거시 코드상 기대값은 "" = prefix 없음)
CANDIDATES=(
  ""
  "education/"
  "edu/"
  "board/"
  "community/"
  "community/edu/"
  "attach/"
  "attachment/"
  "upload/"
  "uploads/"
  "hdrive/"
  "file/"
  "files/"
)

read -r -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
read -r -s -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
echo
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION="${REGION}"

echo
echo "테스트 file_key: ${TEST_KEY}   리전: ${REGION}"
echo "========================================"

found=0
for bkt in "${BUCKETS[@]}"; do
  echo
  echo "[버킷] ${bkt}"

  # 먼저 ListBucket 가능 여부 확인 — 가능하면 실물 몇 개를 직접 나열
  rc=0
  listing="$(aws s3api list-objects-v2 --bucket "${bkt}" --max-items 8 \
                --query 'Contents[].Key' --output text 2>&1)" || rc=$?
  if [[ ${rc} -eq 0 ]]; then
    echo "  [LIST OK] 실제 object key 샘플 (최대 8개):"
    echo "${listing}" | tr '\t' '\n' | sed 's/^/      /'
    echo "      ↑ 이 실물 패턴과 CSV file_key(${TEST_KEY}) 를 비교하세요."
  else
    echo "  [LIST 불가] $(echo "${listing}" | head -1)  → head-object 로 후보 탐색"
  fi

  # prefix 매트릭스 head-object 탐색
  for pfx in "${CANDIDATES[@]}"; do
    key="${pfx}${TEST_KEY}"
    rc=0
    out="$(aws s3api head-object --bucket "${bkt}" --key "${key}" 2>&1)" || rc=$?
    if [[ ${rc} -eq 0 ]]; then
      printf '    [FOUND] s3://%s/%s\n' "${bkt}" "${key}"
      found=1
    elif echo "${out}" | grep -q "Not Found\|404\|NoSuchKey"; then
      :  # 404 는 조용히 (노이즈 억제)
    else
      printf '    [ERR ] prefix=%-14s → %s\n' "'${pfx}'" "$(echo "${out}" | head -1)"
    fi
  done
done

echo
echo "========================================"
if [[ ${found} -eq 1 ]]; then
  echo "→ [FOUND] 조합을 download_attachments.sh 의 BUCKET / S3_PREFIX 에 반영하겠습니다."
else
  echo "→ 모든 버킷×prefix 조합 404."
  echo "  가장 유력: CSV file_key 가 마스킹된 값(empCode→12345678)이라 실제 object key 와 불일치."
  echo "  조치: DB education_post_attachment.file_key 를 마스킹 없이 재추출해 다시 시도."
  echo "  (위 [LIST OK] 샘플이 보였다면 그 실물 key 패턴과 CSV 값을 비교해 확정)"
fi
