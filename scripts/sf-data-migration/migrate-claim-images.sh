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
# S3 업로드는 기본적으로 AWS 콘솔에서 사용자가 직접 수행한다 (aws CLI 미사용).
# 스크립트는 콘솔에 그대로 올릴 로컬 산출물을 준비하고, 업로드 대상 경로를 안내한다.
# (aws CLI 로 자동 업로드까지 원하면 --aws-upload 플래그.)
#
# 산출물 위치 (<out> = input/claim-images/):
#   기존 input/upload_files.csv (레거시 UploadFile__c 추출분) 과 파일명이 겹치므로, 클레임
#   산출물은 input/claim-images/ 하위에 격리한다. Stage1 적재도 클레임 전용 별도 S3 prefix
#   (--stage1-prefix) 로 독립 실행하여 레거시 upload_files.csv 와 섞이지 않게 한다.
#
# 파이프라인 (단계):
#   1) query      sf data query (ContentVersion 메타) → <out>/contentversion-claim.csv
#   2) download   메타 CSV 행별 sf api request (VersionData) → <out>/images/{CV.Id}.{ext} (증분)
#   3) build-csv  build-claim-upload-files.main.kts → <out>/upload_files.csv (메타 변환)
#   --- 여기까지가 기본. 이후 S3 업로드는 콘솔 수동 (안내 출력) ---
#   (opt --aws-upload) aws s3 cp <out>/images/ 와 upload_files.csv 를 자동 업로드
#   (opt --trigger)    backend Stage1 copy-from-s3(UploadFile) → polling → Stage2 resolve
#
# 사전 준비:
#   - sf CLI 인증:   sf org login web --alias <alias>   (또는 sf org list 로 확인)
#   - (--aws-upload 시) AWS 자격증명:  aws configure / AWS_PROFILE / 환경변수
#   - (--trigger 시)   backend API 토큰 — --token <JWT> 또는 env BACKEND_TOKEN, --api-base <url>
#
# 사용법:
#   ./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix>
#       → 로컬 준비 + 콘솔 업로드 안내 (S3 업로드는 콘솔에서 직접)
#   ./migrate-claim-images.sh --org <alias> --bucket <s3-bucket> --stage1-prefix <prefix> --aws-upload
#       → aws CLI 로 S3 업로드까지 자동
#   ./migrate-claim-images.sh ... --skip-query --skip-download     # 변환만 재시도
#   ./migrate-claim-images.sh ... --count-only                     # 추출 대상 건수만 확인
#   ./migrate-claim-images.sh ... --limit 100                      # 샘플 100건만 추출
#   ./migrate-claim-images.sh ... --parallel 12                    # 다운로드 12개 동시 (수십시간→수시간)
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
# 클레임 이미지만 적재 — 이미지 확장자 화이트리스트 (PDF 등 비이미지 첨부 제외).
IMAGE_EXTS="jpg,jpeg,png,gif,bmp,webp,heic,heif"
STAGE1_PREFIX=""
# 산출물(메타 CSV / 이미지 / upload_files.csv)을 두는 전용 하위폴더.
# input/ 아래 두되, 기존 input/upload_files.csv (레거시 UploadFile__c 추출분) 과 파일명이
# 겹치므로 반드시 claim-images/ 하위에 격리한다 (덮어쓰기 방지). Stage1 은 클레임 전용
# 별도 S3 prefix 로 독립 실행하여 레거시 upload_files.csv 와 분리 적재한다.
OUT_DIR="$SCRIPT_DIR/input/claim-images"

SKIP_QUERY=0
SKIP_DOWNLOAD=0
SKIP_BUILD_CSV=0
AWS_UPLOAD=0
DO_TRIGGER=0
COUNT_ONLY=0
LIMIT=""
# 다운로드 동시 실행 수 (1=순차). SF API rate limit 내에서 8~16 권장.
PARALLEL=1

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
        --image-exts)    IMAGE_EXTS="$2"; shift 2 ;;
        --stage1-prefix) STAGE1_PREFIX="${2%/}"; shift 2 ;;
        --out-dir)       OUT_DIR="$2"; shift 2 ;;
        --skip-query)       SKIP_QUERY=1; shift ;;
        --skip-download)    SKIP_DOWNLOAD=1; shift ;;
        --skip-build-csv)   SKIP_BUILD_CSV=1; shift ;;
        --aws-upload)    AWS_UPLOAD=1; shift ;;
        --trigger)       DO_TRIGGER=1; shift ;;
        --count-only)    COUNT_ONLY=1; shift ;;
        --limit)         LIMIT="$2"; shift 2 ;;
        --parallel)      PARALLEL="$2"; shift 2 ;;
        --api-base)      API_BASE="$2"; shift 2 ;;
        --token)         TOKEN="$2"; shift 2 ;;
        -h|--help)       usage; exit 0 ;;
        *) echo "Unknown arg: $1" >&2; usage >&2; exit 1 ;;
    esac
done

# -----------------------------------------------------------------------------
# 검증
# -----------------------------------------------------------------------------

if [[ -n "$LIMIT" && ! "$LIMIT" =~ ^[0-9]+$ ]]; then
    echo "[error] --limit 은 양의 정수여야 함: $LIMIT" >&2; exit 1
fi
if [[ ! "$PARALLEL" =~ ^[0-9]+$ || "$PARALLEL" -lt 1 ]]; then
    echo "[error] --parallel 은 1 이상의 정수여야 함: $PARALLEL" >&2; exit 1
fi

# bucket / stage1-prefix 는 콘솔 업로드 경로 안내 + (옵션) aws-upload / trigger 에 쓰인다.
# --count-only 는 건수 확인만이라 둘 다 불요.
if [[ "$COUNT_ONLY" -eq 0 ]]; then
    if [[ -z "$BUCKET" ]]; then
        echo "[error] --bucket 필수 (콘솔 업로드 경로 안내 + Stage1 s3Bucket)" >&2; exit 1
    fi
    if [[ -z "$STAGE1_PREFIX" ]]; then
        echo "[error] --stage1-prefix 필수 (upload_files.csv 를 둘 Stage1 CSV prefix)" >&2; exit 1
    fi
fi

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

META_CSV="$OUT_DIR/contentversion-claim.csv"
IMG_DIR="$OUT_DIR/images"
UPLOAD_CSV="$OUT_DIR/upload_files.csv"

mkdir -p "$OUT_DIR" "$IMG_DIR"

echo "[info] out dir       : $OUT_DIR"
echo "[info] bucket        : $BUCKET"
echo "[info] image prefix  : $IMAGE_PREFIX"
echo "[info] stage1 prefix : $STAGE1_PREFIX"
echo "[info] sf org        : ${SF_ORG:-(default)}"
echo "[info] api version   : $SF_API_VERSION"
echo "[info] s3 upload      : $([[ "$AWS_UPLOAD" -eq 1 ]] && echo 'aws CLI (자동)' || echo '콘솔 수동 (안내만)')"
echo "[info] image exts     : $IMAGE_EXTS"
echo "[info] parallel      : $PARALLEL"
echo "[info] limit         : ${LIMIT:-(전체)}"
echo

# 클레임 첨부 추출 — claim↔파일 연결은 ContentDocumentLink.LinkedEntityId = claim.Id 다.
# (ContentVersion.RecordId__c / Type__c 는 운영 미사용. SF UI 의 Util.contentdocument 조회 경로와 동일.)
# 각 ContentDocument 의 최신 버전(LatestPublishedVersionId)을 다운로드 단위로 쓴다.
# 이미지 여부는 다운로드/build-csv 단계에서 ContentDocument.FileExtension(화이트리스트)로 거른다.
CDL_WHERE="WHERE LinkedEntityId IN (SELECT Id FROM DKRetail__Claim__c)"
CDL_SELECT="SELECT LinkedEntityId, ContentDocumentId, ContentDocument.LatestPublishedVersionId, ContentDocument.Title, ContentDocument.FileExtension, ContentDocument.ContentSize, ContentDocument.CreatedDate, ContentDocument.LastModifiedDate FROM ContentDocumentLink $CDL_WHERE"
if [[ -n "$LIMIT" ]]; then
    CDL_SELECT="$CDL_SELECT LIMIT $LIMIT"
fi

# -----------------------------------------------------------------------------
# 추출 대상 건수 미리보기 (항상 출력)
# -----------------------------------------------------------------------------

echo "[count] 추출 대상 건수 확인 중..."
total_target="$(sf data query \
    --query "SELECT COUNT() FROM ContentDocumentLink $CDL_WHERE" \
    --result-format json \
    --api-version "$SF_API_VERSION" \
    ${SF_ORG_ARGS[@]+"${SF_ORG_ARGS[@]}"} | python3 -c 'import sys,json; print(json.load(sys.stdin).get("result",{}).get("totalSize",""))')"
echo "[count] 클레임에 링크된 ContentDocumentLink 전체: ${total_target} 건"
echo "[count] (이 중 이미지 확장자만 적재됨 — 비이미지 첨부는 build-csv 에서 제외)"
if [[ -n "$LIMIT" ]]; then
    echo "[count] 이번 실행 추출 대상(--limit): $LIMIT 건 (전체 중 일부)"
fi
echo

if [[ "$COUNT_ONLY" -eq 1 ]]; then
    echo "[done] --count-only — 건수만 확인하고 종료."
    exit 0
fi

# -----------------------------------------------------------------------------
# 1) query — ContentDocumentLink 메타 CSV
# -----------------------------------------------------------------------------

if [[ "$SKIP_QUERY" -eq 1 ]]; then
    echo "[skip] query"
    if [[ ! -f "$META_CSV" ]]; then
        echo "[error] --skip-query 인데 메타 CSV 없음: $META_CSV" >&2; exit 1
    fi
else
    echo "[step 1/3] query ContentDocumentLink 메타 → $META_CSV${LIMIT:+ (LIMIT $LIMIT)}"
    sf data query \
        --query "$CDL_SELECT" \
        --result-format csv \
        --api-version "$SF_API_VERSION" \
        ${SF_ORG_ARGS[@]+"${SF_ORG_ARGS[@]}"} > "$META_CSV"
    echo "[ok] meta rows: $(($(wc -l < "$META_CSV") - 1))"
fi
echo

# -----------------------------------------------------------------------------
# 2) download — 최신 ContentVersion(VersionData) 바이너리 (이미지만, 증분, 병렬)
# -----------------------------------------------------------------------------
# 이미지 판정/확장자 계산은 python 단계에서 수행하고 (build-csv 와 동일 기준), 다운로드는
# xargs -P <PARALLEL> 로 동시 실행한다. 워커는 bash -c 라 환경변수로 컨텍스트를 전달한다.
# 이미 받은 파일은 워커가 skip → 증분/재실행 안전. PARALLEL=1 이면 순차와 동일.

if [[ "$SKIP_DOWNLOAD" -eq 1 ]]; then
    echo "[skip] download"
else
    echo "[step 2/3] download VersionData (이미지만, parallel=$PARALLEL) → $IMG_DIR (증분)"

    # 다운로드 워커가 참조할 컨텍스트 export.
    export IMG_DIR SF_API_VERSION SF_ORG IMAGE_EXTS

    # python: 메타 CSV → 이미지 대상만 "cvId.ext" 한 토큰/줄 출력 (공백 없음 → xargs 안전).
    #         total/image/non-image 카운트는 stderr 로.
    count_tmp="$(mktemp)"
    targets="$(python3 -c '
import csv, os, sys
exts = set(e.strip().lower() for e in os.environ.get("IMAGE_EXTS","").split(",") if e.strip())
total = img = nonimg = 0
out = []
with open(sys.argv[1], newline="") as f:
    for row in csv.DictReader(f):
        total += 1
        cv = row.get("ContentDocument.LatestPublishedVersionId","").strip()
        fe = row.get("ContentDocument.FileExtension","").strip().lstrip(".").lower()
        title = row.get("ContentDocument.Title","").strip()
        if not cv:
            continue
        ext = fe
        if not ext:
            ext = title.rsplit(".",1)[1].lower() if "." in title else "jpg"
        if ext not in exts:
            nonimg += 1
            continue
        img += 1
        out.append(cv + "." + ext)
sys.stderr.write("%d %d %d\n" % (total, img, nonimg))
print("\n".join(out))
' "$META_CSV" 2>"$count_tmp")"
    read -r c_total c_img c_nonimg < "$count_tmp"; rm -f "$count_tmp"

    before=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
    SECONDS=0
    dl_rc=0
    # 워커: 파일명(cvId.ext) → 존재 skip → VersionData 스트림 다운로드.
    printf '%s\n' "$targets" | grep -v '^$' | xargs -P "$PARALLEL" -I {} bash -c '
        f="$1"; cvId="${f%.*}"; out="$IMG_DIR/$f"
        [ -f "$out" ] && exit 0
        if [ -n "$SF_ORG" ]; then
            sf api request rest "/services/data/v$SF_API_VERSION/sobjects/ContentVersion/$cvId/VersionData" --stream-to-file "$out" --target-org "$SF_ORG" >/dev/null
        else
            sf api request rest "/services/data/v$SF_API_VERSION/sobjects/ContentVersion/$cvId/VersionData" --stream-to-file "$out" >/dev/null
        fi
    ' _ {} || dl_rc=$?

    after=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
    dl=$((after - before))
    echo "[ok] meta=$c_total  image-target=$c_img  non-image=$c_nonimg"
    echo "[ok] downloaded(new)=$dl  total-on-disk=$after  elapsed=${SECONDS}s  parallel=$PARALLEL"
    if [[ "$dl_rc" -ne 0 ]]; then
        echo "[warn] 일부 다운로드 실패 (xargs rc=$dl_rc). 증분이라 재실행하면 실패분만 다시 받습니다." >&2
    fi
fi
echo

# -----------------------------------------------------------------------------
# 3) build-csv — 메타 CSV → upload_files.csv
# -----------------------------------------------------------------------------

if [[ "$SKIP_BUILD_CSV" -eq 1 ]]; then
    echo "[skip] build-csv"
    if [[ ! -f "$UPLOAD_CSV" ]]; then
        echo "[error] --skip-build-csv 인데 upload_files.csv 없음: $UPLOAD_CSV" >&2; exit 1
    fi
else
    echo "[step 3/3] build upload_files.csv → $UPLOAD_CSV"
    kotlinc -script "$SCRIPT_DIR/build-claim-upload-files.main.kts" -- \
        --meta-csv "$META_CSV" \
        --out "$UPLOAD_CSV" \
        --image-prefix "$IMAGE_PREFIX"
fi
echo

img_count=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')

# -----------------------------------------------------------------------------
# S3 업로드 — 기본: 콘솔 수동 안내 / --aws-upload: aws CLI 자동
# -----------------------------------------------------------------------------

if [[ "$AWS_UPLOAD" -eq 1 ]]; then
    echo "[s3-upload] aws CLI 로 업로드"
    echo "  - images → s3://$BUCKET/$IMAGE_PREFIX/"
    aws s3 cp "$IMG_DIR/" "s3://$BUCKET/$IMAGE_PREFIX/" --recursive
    echo "  - csv    → s3://$BUCKET/$STAGE1_PREFIX/upload_files.csv"
    aws s3 cp "$UPLOAD_CSV" "s3://$BUCKET/$STAGE1_PREFIX/upload_files.csv"
    echo "[ok] s3 업로드 완료"
else
    echo "════════════════════════════════════════════════════════════════"
    echo " 로컬 산출물 준비 완료 — AWS 콘솔에서 아래 두 가지를 업로드하세요"
    echo "════════════════════════════════════════════════════════════════"
    echo
    echo " ① 클레임 이미지 ($img_count 개)"
    echo "    로컬 : $IMG_DIR/   (폴더 안 파일들)"
    echo "    대상 : s3://$BUCKET/$IMAGE_PREFIX/"
    echo "           (콘솔에서 '$IMAGE_PREFIX/' 폴더로 들어가 파일 업로드)"
    echo
    echo " ② Stage1 적재용 CSV"
    echo "    로컬 : $UPLOAD_CSV"
    echo "    대상 : s3://$BUCKET/$STAGE1_PREFIX/upload_files.csv"
    echo "           (콘솔에서 '$STAGE1_PREFIX/' 폴더로 들어가 upload_files.csv 업로드)"
    echo
    echo " 주의: 이미지 파일명(= {ContentVersion.Id}.{ext})을 바꾸지 마세요."
    echo "       upload_files.csv 의 UniqueKey__c 와 1:1 로 맞물립니다."
    echo "════════════════════════════════════════════════════════════════"
fi
echo

# -----------------------------------------------------------------------------
# trigger (옵션) — backend Stage1 (UploadFile) → polling → Stage2 resolve
# -----------------------------------------------------------------------------

if [[ "$DO_TRIGGER" -eq 0 ]]; then
    echo "다음 단계 — web SF Migration 화면에서 (S3 업로드 완료 후):"
    echo "  1) Stage1 copy-from-s3 (target=UploadFile, s3KeyPrefix=$STAGE1_PREFIX)"
    echo "  2) Stage2 'UploadFile Parent Resolve'"
    echo "  (또는 다시 실행: --trigger --api-base <url> --token <JWT>)"
    exit 0
fi

if [[ -z "$API_BASE" || -z "$TOKEN" ]]; then
    echo "[error] --trigger 에는 --api-base 와 --token (또는 env BACKEND_API_BASE/BACKEND_TOKEN) 필요" >&2
    exit 1
fi

echo "[trigger] Stage1 + Stage2 via $API_BASE (S3 업로드가 끝났다고 가정)"

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
