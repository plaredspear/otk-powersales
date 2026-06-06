#!/usr/bin/env bash
#
# 클레임 이미지 마이그레이션 오케스트레이션 (반복 실행 가능, 사용자 수동 실행)
#
# 배경:
#   클레임(DKRetail__Claim__c) 첨부 이미지는 레거시에서 SF Files(ContentVersion)에만 저장되었고
#   (IF_REST_MOBILE_ClaimRegist), UploadFile__c / S3 에는 적재된 적이 없다. 신규 시스템은 claim
#   이미지를 upload_file (parent_type='Claim', unique_key=S3 key) 로 조회하므로, ContentVersion 을
#   추출 → S3 재업로드 → upload_file 적재해야 한다. cut-over 전까지 반복 실행 가능하도록 전 단계를
#   스크립트화한다 (각 단계 멱등 / --skip-* 로 부분 재시도).
#
# 파이프라인 (단계):
#   1) query      sf data query (ContentVersion 메타) → <out>/contentversion-claim.csv
#   2) download   메타 CSV 행별 sf api request (VersionData) → <out>/bin/{CV.Id}.{ext} (증분)
#   3) s3-images  aws s3 cp <out>/bin/ s3://<bucket>/<image-prefix>/ (이미지 재업로드)
#   4) build-csv  build-claim-upload-files.main.kts → <out>/upload_files.csv (메타 변환)
#   5) upload-csv aws s3 cp upload_files.csv s3://<bucket>/<stage1-prefix>/upload_files.csv
#   6) trigger    (옵션 --trigger) backend Stage1 copy-from-s3(UploadFile) → polling → Stage2 resolve
#
# 사전 준비:
#   - sf CLI 인증:   sf org login web --alias <alias>   (또는 sf org list 로 확인)
#   - AWS 자격증명:  aws configure / AWS_PROFILE / 환경변수 (s3 cp 사용)
#   - (--trigger 시) backend API 토큰 — --token <JWT> 또는 env BACKEND_TOKEN, --api-base <url>
#
# 사용법:
#   ./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix>
#   ./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> \
#       --trigger --api-base https://<host> --token <JWT>
#   ./migrate-claim-images.sh ... --skip-query --skip-download   # 변환/업로드만 재시도
#
# SF/AWS CLI 자발 호출 금지 정책: 본 스크립트는 sf/aws 를 래핑하지만 실행 주체는 사용자다.

set -euo pipefail

# -----------------------------------------------------------------------------
# 기본값 / 인자 파싱
# -----------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SF_ORG=""
SF_API_VERSION="60.0"
BUCKET=""
IMAGE_PREFIX="uploads/claim/migrated"
STAGE1_PREFIX=""
OUT_DIR="$SCRIPT_DIR/output/claim-images"

SKIP_QUERY=0
SKIP_DOWNLOAD=0
SKIP_S3=0
SKIP_BUILD_CSV=0
SKIP_UPLOAD_CSV=0
DO_TRIGGER=0

API_BASE="${BACKEND_API_BASE:-}"
TOKEN="${BACKEND_TOKEN:-}"

usage() {
    sed -n '2,40p' "$0"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --org)           SF_ORG="$2"; shift 2 ;;
        --api-version)   SF_API_VERSION="$2"; shift 2 ;;
        --bucket)        BUCKET="$2"; shift 2 ;;
        --image-prefix)  IMAGE_PREFIX="${2%/}"; shift 2 ;;
        --stage1-prefix) STAGE1_PREFIX="${2%/}"; shift 2 ;;
        --out-dir)       OUT_DIR="$2"; shift 2 ;;
        --skip-query)       SKIP_QUERY=1; shift ;;
        --skip-download)    SKIP_DOWNLOAD=1; shift ;;
        --skip-s3)          SKIP_S3=1; shift ;;
        --skip-build-csv)   SKIP_BUILD_CSV=1; shift ;;
        --skip-upload-csv)  SKIP_UPLOAD_CSV=1; shift ;;
        --trigger)       DO_TRIGGER=1; shift ;;
        --api-base)      API_BASE="$2"; shift 2 ;;
        --token)         TOKEN="$2"; shift 2 ;;
        -h|--help)       usage; exit 0 ;;
        *) echo "Unknown arg: $1" >&2; usage >&2; exit 1 ;;
    esac
done

# -----------------------------------------------------------------------------
# 검증
# -----------------------------------------------------------------------------

if [[ -z "$BUCKET" ]]; then
    echo "[error] --bucket 필수" >&2; exit 1
fi
if [[ -z "$STAGE1_PREFIX" && "$SKIP_UPLOAD_CSV" -eq 0 ]]; then
    echo "[error] --stage1-prefix 필수 (--skip-upload-csv 시 생략 가능)" >&2; exit 1
fi

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

META_CSV="$OUT_DIR/contentversion-claim.csv"
BIN_DIR="$OUT_DIR/bin"
UPLOAD_CSV="$OUT_DIR/upload_files.csv"

mkdir -p "$OUT_DIR" "$BIN_DIR"

echo "[info] out dir       : $OUT_DIR"
echo "[info] bucket        : $BUCKET"
echo "[info] image prefix  : $IMAGE_PREFIX"
echo "[info] stage1 prefix : ${STAGE1_PREFIX:-(skip)}"
echo "[info] sf org        : ${SF_ORG:-(default)}"
echo "[info] api version   : $SF_API_VERSION"
echo

# ContentVersion 추출 SOQL — 클레임 첨부만 (Type__c 3종 + RecordId__c 보유).
CONTENTVERSION_SOQL="SELECT Id, RecordId__c, Type__c, Title, PathOnClient, FileExtension, ContentSize, CreatedDate, LastModifiedDate FROM ContentVersion WHERE Type__c IN ('클레임','일부인','영수증') AND RecordId__c != null"

# -----------------------------------------------------------------------------
# 1) query — ContentVersion 메타 CSV
# -----------------------------------------------------------------------------

if [[ "$SKIP_QUERY" -eq 1 ]]; then
    echo "[skip] query"
    if [[ ! -f "$META_CSV" ]]; then
        echo "[error] --skip-query 인데 메타 CSV 없음: $META_CSV" >&2; exit 1
    fi
else
    echo "[step 1/6] query ContentVersion 메타 → $META_CSV"
    sf data query \
        --query "$CONTENTVERSION_SOQL" \
        --result-format csv \
        --api-version "$SF_API_VERSION" \
        "${SF_ORG_ARGS[@]}" > "$META_CSV"
    echo "[ok] meta rows: $(($(wc -l < "$META_CSV") - 1))"
fi
echo

# -----------------------------------------------------------------------------
# 2) download — VersionData 바이너리 (증분: 이미 있는 파일 skip)
# -----------------------------------------------------------------------------
# 메타 CSV 컬럼 인덱스: Id, RecordId__c, Type__c, Title, PathOnClient, FileExtension, ...

ext_of() {
    # $1=FileExtension $2=PathOnClient
    local fe="$1" poc="$2"
    fe="${fe#.}"
    if [[ -n "$fe" ]]; then echo "${fe,,}"; return; fi
    if [[ "$poc" == *.* ]]; then echo "${poc##*.}" | tr '[:upper:]' '[:lower:]'; return; fi
    echo "jpg"
}

if [[ "$SKIP_DOWNLOAD" -eq 1 ]]; then
    echo "[skip] download"
else
    echo "[step 2/6] download VersionData → $BIN_DIR (증분)"
    local_dl=0; local_skip=0
    # CSV 파싱 — Id(1), FileExtension(6), PathOnClient(5) 위치는 SOQL SELECT 순서 기준.
    # OpenCSV 안 쓰고 awk 로 따옴표 처리: sf csv 는 RFC4180. 안전을 위해 python3 로 파싱.
    while IFS=$'\t' read -r cvId fileExt pathOnClient; do
        [[ -z "$cvId" ]] && continue
        ext="$(ext_of "$fileExt" "$pathOnClient")"
        outfile="$BIN_DIR/$cvId.$ext"
        if [[ -f "$outfile" ]]; then
            local_skip=$((local_skip + 1))
            continue
        fi
        sf api request rest \
            "/services/data/v$SF_API_VERSION/sobjects/ContentVersion/$cvId/VersionData" \
            --stream-to-file "$outfile" \
            "${SF_ORG_ARGS[@]}" >/dev/null
        local_dl=$((local_dl + 1))
    done < <(python3 -c '
import csv, sys
with open(sys.argv[1], newline="") as f:
    r = csv.DictReader(f)
    for row in r:
        print("\t".join([row.get("Id","").strip(), row.get("FileExtension","").strip(), row.get("PathOnClient","").strip()]))
' "$META_CSV")
    echo "[ok] downloaded=$local_dl  skipped(existing)=$local_skip"
fi
echo

# -----------------------------------------------------------------------------
# 3) s3-images — 바이너리 S3 재업로드
# -----------------------------------------------------------------------------

if [[ "$SKIP_S3" -eq 1 ]]; then
    echo "[skip] s3-images"
else
    echo "[step 3/6] upload images → s3://$BUCKET/$IMAGE_PREFIX/"
    aws s3 cp "$BIN_DIR/" "s3://$BUCKET/$IMAGE_PREFIX/" --recursive
    echo "[ok] images uploaded"
fi
echo

# -----------------------------------------------------------------------------
# 4) build-csv — 메타 CSV → upload_files.csv
# -----------------------------------------------------------------------------

if [[ "$SKIP_BUILD_CSV" -eq 1 ]]; then
    echo "[skip] build-csv"
    if [[ ! -f "$UPLOAD_CSV" ]]; then
        echo "[error] --skip-build-csv 인데 upload_files.csv 없음: $UPLOAD_CSV" >&2; exit 1
    fi
else
    echo "[step 4/6] build upload_files.csv → $UPLOAD_CSV"
    kotlinc -script "$SCRIPT_DIR/build-claim-upload-files.main.kts" -- \
        --meta-csv "$META_CSV" \
        --out "$UPLOAD_CSV" \
        --image-prefix "$IMAGE_PREFIX"
fi
echo

# -----------------------------------------------------------------------------
# 5) upload-csv — upload_files.csv 를 Stage1 prefix 로 업로드
# -----------------------------------------------------------------------------

if [[ "$SKIP_UPLOAD_CSV" -eq 1 ]]; then
    echo "[skip] upload-csv"
else
    echo "[step 5/6] upload CSV → s3://$BUCKET/$STAGE1_PREFIX/upload_files.csv"
    aws s3 cp "$UPLOAD_CSV" "s3://$BUCKET/$STAGE1_PREFIX/upload_files.csv"
    echo "[ok] upload_files.csv uploaded"
fi
echo

# -----------------------------------------------------------------------------
# 6) trigger (옵션) — backend Stage1 (UploadFile) → polling → Stage2 resolve
# -----------------------------------------------------------------------------

if [[ "$DO_TRIGGER" -eq 0 ]]; then
    echo "[step 6/6] trigger SKIPPED (--trigger 미지정)"
    echo
    echo "다음 단계 — web SF Migration 화면에서 수동 실행:"
    echo "  1) Stage1 copy-from-s3 (target=UploadFile, s3KeyPrefix=$STAGE1_PREFIX)"
    echo "  2) Stage2 'UploadFile Parent Resolve'"
    exit 0
fi

if [[ -z "$API_BASE" || -z "$TOKEN" ]]; then
    echo "[error] --trigger 에는 --api-base 와 --token (또는 env BACKEND_API_BASE/BACKEND_TOKEN) 필요" >&2
    exit 1
fi

echo "[step 6/6] trigger Stage1 + Stage2 via $API_BASE"

auth_hdr=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")

# Stage1 copy-from-s3 (UploadFile)
echo "  - Stage1 copy-from-s3 (UploadFile)"
curl -fsS -X POST "$API_BASE/api/v1/admin/sf-migration/stage1/copy-from-s3" \
    "${auth_hdr[@]}" \
    -d "{\"targetName\":\"UploadFile\",\"s3Bucket\":\"$BUCKET\",\"s3KeyPrefix\":\"$STAGE1_PREFIX\"}" >/dev/null

# polling — RUNNING 동안 대기
echo -n "  - polling Stage1 progress"
while :; do
    sleep 3
    status="$(curl -fsS "$API_BASE/api/v1/admin/sf-migration/stage1/copy-from-s3/progress" \
        -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("status",""))')"
    echo -n "."
    [[ "$status" != "RUNNING" ]] && { echo " -> $status"; break; }
done

# Stage2 upload-file-polymorphic-parent
echo "  - Stage2 upload-file-polymorphic-parent"
curl -fsS -X POST "$API_BASE/api/v1/admin/sf-migration/stage2/upload-file-polymorphic-parent" \
    "${auth_hdr[@]}" -d '{}'
echo
echo "[done] trigger 완료"
