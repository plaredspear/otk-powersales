#!/usr/bin/env bash
#
# 제품 클레임 이미지 public/ → private/ 이동 안내 (멱등, 사용자 수동 실행)
#
# 배경:
#   제품 클레임(Claim) 이미지는 권한 통제 대상이라 신규에서 S3 private/ 폴더 + presigned URL
#   조회로 전환되었다 (backend storage 레이어). 전환 이전에 적재된 기존 claim 이미지는 S3
#   public/ 하위(public/uploads/claim/...)에 있으므로, private/ 하위로 이동해야 backend 의
#   presigned 조회(실 객체 key = private/ + uniqueKey)와 정합한다.
#
#   DB upload_file.unique_key 는 segment 를 포함하지 않으므로(= uploads/claim/...) **DB 변경
#   불필요**. S3 객체 위치만 옮기면 된다.
#
# 역할 분담 (AWS CLI 자발 호출 금지 정책):
#   본 스크립트는 실행해야 할 aws s3 mv 명령을 **출력·안내**만 한다. 실제 이동은 사용자가
#   AWS 콘솔 또는 CLI 로 직접 수행한다. 멱등: aws s3 mv 는 이미 옮겨진 객체의 source 부재 시
#   no-op 이므로 재실행 안전.
#
# 무중단 cut-over: 불필요 (운영 정책). backend 배포와 S3 이동 사이 짧은 공백 중 일부 claim
#   이미지 조회 실패는 허용. 무중단이 필요하면 mv 대신 cp(원본 유지) → 배포 → 검증 → rm 으로
#   분리 가능 (주석 하단 참고).
#
# 사용법:
#   ./migrate-claim-to-private.sh --bucket <S3_BUCKET>
#       → public/uploads/claim/ → private/uploads/claim/ 이동 명령 + 검증 명령 출력
#   ./migrate-claim-to-private.sh --bucket <S3_BUCKET> --include-migrated
#       → migrated/ 하위(public/uploads/claim/migrated/) 포함 안내
#
# 사전 확인:
#   - 버킷 정책에서 private/ 가 anonymous read 불가인지 콘솔에서 확인 (이미 그렇게 운영 중).

set -euo pipefail

# -----------------------------------------------------------------------------
# 인자 파싱
# -----------------------------------------------------------------------------

BUCKET=""
INCLUDE_MIGRATED=false

usage() {
	cat <<'USAGE'
사용법: migrate-claim-to-private.sh --bucket <S3_BUCKET> [--include-migrated]

  --bucket <name>       대상 S3 버킷명 (필수). backend app.aws.s3.bucket 와 동일.
  --include-migrated    public/uploads/claim/migrated/ 하위도 함께 안내.
  -h, --help            이 도움말.
USAGE
}

while [[ $# -gt 0 ]]; do
	case "$1" in
		--bucket) BUCKET="${2:-}"; shift 2 ;;
		--include-migrated) INCLUDE_MIGRATED=true; shift ;;
		-h|--help) usage; exit 0 ;;
		*) echo "알 수 없는 인자: $1" >&2; usage; exit 1 ;;
	esac
done

if [[ -z "$BUCKET" ]]; then
	echo "오류: --bucket 은 필수입니다." >&2
	usage
	exit 1
fi

SRC_PREFIX="public/uploads/claim"
DST_PREFIX="private/uploads/claim"

# -----------------------------------------------------------------------------
# 안내 출력
# -----------------------------------------------------------------------------

cat <<EOF
============================================================================
 제품 클레임 이미지 public/ → private/ 이동 안내
============================================================================
 버킷        : ${BUCKET}
 이동 전     : s3://${BUCKET}/${SRC_PREFIX}/
 이동 후     : s3://${BUCKET}/${DST_PREFIX}/
 DB 변경     : 없음 (upload_file.unique_key 는 segment 미포함)
----------------------------------------------------------------------------

[1] 이동 전 대상 건수 확인 (사용자 실행):

  aws s3 ls s3://${BUCKET}/${SRC_PREFIX}/ --recursive | wc -l

[2] public/ → private/ 이동 (사용자 실행, 멱등):

  aws s3 mv s3://${BUCKET}/${SRC_PREFIX}/ s3://${BUCKET}/${DST_PREFIX}/ --recursive
EOF

if [[ "$INCLUDE_MIGRATED" == true ]]; then
	cat <<EOF

  # migrated/ 하위는 [2] 의 --recursive 에 이미 포함됨 (public/uploads/claim/migrated/...).
  # 별도 prefix 로 적재된 경우에만 추가 실행:
  # aws s3 mv s3://${BUCKET}/public/uploads/claim/migrated/ s3://${BUCKET}/private/uploads/claim/migrated/ --recursive
EOF
fi

cat <<EOF

[3] 이동 후 검증 (사용자 실행):

  # private/ 건수가 이동 전 public/ 건수와 일치하는지 확인
  aws s3 ls s3://${BUCKET}/${DST_PREFIX}/ --recursive | wc -l

  # public/ 하위에 claim 잔여 객체가 없는지 확인 (0 이어야 함)
  aws s3 ls s3://${BUCKET}/${SRC_PREFIX}/ --recursive | wc -l

[4] 버킷 정책 확인 (콘솔):

  private/ prefix 가 anonymous(public) read 불가인지 확인.
  presigned URL 없이 https://${BUCKET}.../private/uploads/claim/... 직접 접근 시
  AccessDenied(403) 가 떠야 정상.

----------------------------------------------------------------------------
 무중단이 필요하면 [2] 대신 cp(원본 유지) 후 배포·검증·삭제로 분리:
   aws s3 cp s3://${BUCKET}/${SRC_PREFIX}/ s3://${BUCKET}/${DST_PREFIX}/ --recursive
   # → backend 배포 → [3] 검증 → 정상 확인 후:
   aws s3 rm s3://${BUCKET}/${SRC_PREFIX}/ --recursive
============================================================================
EOF
