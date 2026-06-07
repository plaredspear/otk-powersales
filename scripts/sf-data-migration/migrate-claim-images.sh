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
# 역할 분담:
#   본 스크립트는 **로컬 산출물 준비 + 콘솔 업로드 경로 안내**까지만 담당한다.
#   - S3 업로드     : AWS 콘솔에서 사용자가 직접 (스크립트는 prefix 경로만 안내).
#   - Stage1/Stage2 : web 의 SF Migration 화면에서 처리. bucket 은 web 이 backend 프리필
#                     (app.aws.s3.bucket) 로 자동 표시하므로 본 스크립트는 bucket 을 다루지 않는다.
#
# 산출물 위치 (<out> = input/claim-images/):
#   기존 input/upload_files.csv (레거시 UploadFile__c 추출분) 과 파일명이 겹치므로, 클레임
#   산출물은 input/claim-images/ 하위에 격리한다. Stage1 적재도 클레임 전용 별도 S3 prefix
#   (--stage1-prefix) 로 독립 실행하여 레거시 upload_files.csv 와 섞이지 않게 한다.
#
# 파이프라인 (단계):
#   1) query      sf data query (ContentVersion 메타) → <out>/contentversion-claim.csv
#   2) download   메타 CSV 행별 sf api request (VersionData) → <out>/images/{CV.Id}.{ext} (증분)
#   3) build-csv  build-claim-upload-files.main.kts → <out>/claim_upload_files.csv (메타 변환)
#   --- 이후 S3 업로드(콘솔) → web Stage1/Stage2 는 안내만 출력 ---
#
# 사전 준비:
#   - sf CLI 인증:   sf org login web --alias <alias>   (또는 sf org list 로 확인)
#
# 사용법:
#   ./migrate-claim-images.sh --org <alias>
#       → 로컬 준비 + 콘솔 업로드 안내 (S3 업로드는 콘솔, Stage1/Stage2 는 web 에서 직접)
#       → Stage1 CSV prefix 는 sf-migration/claim-images 고정 (--stage1-prefix 로 override)
#   ./migrate-claim-images.sh ... --skip-query --skip-download     # 변환만 재시도
#   ./migrate-claim-images.sh ... --count-only                     # 추출 대상 건수만 확인
#   ./migrate-claim-images.sh ... --limit 100                      # 샘플 100건만 추출
#   ./migrate-claim-images.sh ... --parallel 12                    # 다운로드 12개 동시 (수십시간→수시간)
#
# SF CLI 자발 호출 금지 정책: 본 스크립트는 sf 를 래핑하지만 실행 주체는 사용자다.

set -euo pipefail

# -----------------------------------------------------------------------------
# 기본값 / 인자 파싱
# -----------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SF_ORG=""
SF_API_VERSION="60.0"
# upload_file.unique_key 의 prefix (= web/mobile 조회 key). **public/ 을 포함하지 않는다.**
#   조회 시 PublicUrlResolver 가 prefix(S3_PUBLIC_URL_PREFIX, 예: https://<bucket>.s3.../public/)
#   에 unique_key 를 이어붙여 완전 URL 을 만든다. prefix 가 이미 .../public/ 으로 끝나므로
#   unique_key 에 public/ 을 넣으면 public/ 이 중복된다(.../public/public/uploads/...).
#   따라서 unique_key = uploads/claim/migrated/{CV.Id}.{ext}, S3 실제 객체 key = public/ + unique_key.
#   신규 직접 등록(S3StorageService.buildKey)도 동일하게 uploads/... (public 없음) 를 쓴다.
# --image-prefix 로 override 가능.
IMAGE_PREFIX="uploads/claim/migrated"
# S3 객체 key 는 public/ 하위에 위치한다 (anonymous GET 가능 폴더). 콘솔 업로드 대상 폴더 =
# PUBLIC_SEGMENT + "/" + IMAGE_PREFIX. PublicUrlResolver prefix 의 .../public/ 와 정합.
PUBLIC_SEGMENT="public"
# 클레임 이미지만 적재 — 이미지 확장자 화이트리스트 (PDF 등 비이미지 첨부 제외).
IMAGE_EXTS="jpg,jpeg,png,gif,bmp,webp,heic,heif"
# Stage1 CSV prefix — 클레임 전용 고정값. 레거시 Stage1 CSV (sf-migration/input/) 의
# upload_files.csv 와 파일명이 같아 덮어쓰지 않도록 별도 prefix 로 격리한다. backend
# DEFAULT_S3_KEY_PREFIX (sf-migration/input) 와 같은 sf-migration/ 네임스페이스를 따른다.
# --stage1-prefix 로 override 가능 (선택).
STAGE1_PREFIX="sf-migration/claim-images"
# 산출물(메타 CSV / 이미지 / upload_files.csv)을 두는 전용 하위폴더.
# input/ 아래 두되, 기존 input/upload_files.csv (레거시 UploadFile__c 추출분) 과 파일명이
# 겹치므로 반드시 claim-images/ 하위에 격리한다 (덮어쓰기 방지). Stage1 은 클레임 전용
# 별도 S3 prefix 로 독립 실행하여 레거시 upload_files.csv 와 분리 적재한다.
OUT_DIR="$SCRIPT_DIR/input/claim-images"

SKIP_QUERY=0
SKIP_DOWNLOAD=0
SKIP_BUILD_CSV=0
COUNT_ONLY=0
LIMIT=""
# 다운로드 동시 실행 수 (1=순차). SF API rate limit 내에서 8~16 권장.
PARALLEL=1

usage() {
    sed -n '2,40p' "$0"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --org)           SF_ORG="$2"; shift 2 ;;
        --api-version)   SF_API_VERSION="$2"; shift 2 ;;
        --image-prefix)  IMAGE_PREFIX="${2%/}"; shift 2 ;;
        --image-exts)    IMAGE_EXTS="$2"; shift 2 ;;
        --stage1-prefix) STAGE1_PREFIX="${2%/}"; shift 2 ;;
        --out-dir)       OUT_DIR="$2"; shift 2 ;;
        --skip-query)       SKIP_QUERY=1; shift ;;
        --skip-download)    SKIP_DOWNLOAD=1; shift ;;
        --skip-build-csv)   SKIP_BUILD_CSV=1; shift ;;
        --count-only)    COUNT_ONLY=1; shift ;;
        --limit)         LIMIT="$2"; shift 2 ;;
        --parallel)      PARALLEL="$2"; shift 2 ;;
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

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

META_CSV="$OUT_DIR/contentversion-claim.csv"
IMG_DIR="$OUT_DIR/images"
# 파일명은 backend Stage1 의 ClaimImageUploadFile 타겟 csvFileName 과 정합해야 한다
# (레거시 UploadFile 타겟의 upload_files.csv 와 분리 — 독립 타겟/독립 파일명).
UPLOAD_CSV="$OUT_DIR/claim_upload_files.csv"

mkdir -p "$OUT_DIR" "$IMG_DIR"

echo "[info] out dir       : $OUT_DIR"
echo "[info] unique_key prefix : $IMAGE_PREFIX  (DB 적재값, public/ 없음)"
echo "[info] s3 객체 폴더   : $PUBLIC_SEGMENT/$IMAGE_PREFIX/  (콘솔 업로드 대상)"
echo "[info] stage1 prefix : $STAGE1_PREFIX"
echo "[info] sf org        : ${SF_ORG:-(default)}"
echo "[info] api version   : $SF_API_VERSION"
echo "[info] s3 upload     : 콘솔 수동 (안내만)"
echo "[info] image exts    : $IMAGE_EXTS"
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
# 3) build-csv — 메타 CSV → claim_upload_files.csv
# -----------------------------------------------------------------------------

if [[ "$SKIP_BUILD_CSV" -eq 1 ]]; then
    echo "[skip] build-csv"
    if [[ ! -f "$UPLOAD_CSV" ]]; then
        echo "[error] --skip-build-csv 인데 claim_upload_files.csv 없음: $UPLOAD_CSV" >&2; exit 1
    fi
else
    echo "[step 3/3] build claim_upload_files.csv → $UPLOAD_CSV"
    kotlinc -script "$SCRIPT_DIR/build-claim-upload-files.main.kts" -- \
        --meta-csv "$META_CSV" \
        --out "$UPLOAD_CSV" \
        --image-prefix "$IMAGE_PREFIX"
fi
echo

img_count=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')

# -----------------------------------------------------------------------------
# S3 업로드 안내 — 콘솔 수동 (bucket 은 web Stage1 폼에서 backend 프리필로 확인)
# -----------------------------------------------------------------------------

echo "════════════════════════════════════════════════════════════════"
echo " 로컬 산출물 준비 완료 — AWS 콘솔에서 아래 두 가지를 업로드하세요"
echo " (대상 bucket 은 web Stage1 폼의 프리필 값과 동일한 운영 S3 버킷)"
echo "════════════════════════════════════════════════════════════════"
echo
echo " ① 클레임 이미지 ($img_count 개)"
echo "    로컬 : $IMG_DIR/   (폴더 안 파일들)"
echo "    대상 : <운영 S3 버킷>/$PUBLIC_SEGMENT/$IMAGE_PREFIX/"
echo "           (콘솔에서 '$PUBLIC_SEGMENT/$IMAGE_PREFIX/' 폴더로 들어가 파일 업로드)"
echo "           ※ unique_key 는 public/ 없이 '$IMAGE_PREFIX/...' 이고, S3 객체 key 는"
echo "             public/ 하위입니다 (PublicUrlResolver prefix 가 .../public/ 로 끝남)."
echo
echo " ② Stage1 적재용 CSV"
echo "    로컬 : $UPLOAD_CSV"
echo "    대상 : <운영 S3 버킷>/$STAGE1_PREFIX/claim_upload_files.csv"
echo "           (콘솔에서 '$STAGE1_PREFIX/' 폴더로 들어가 claim_upload_files.csv 업로드)"
echo
echo " 주의: 이미지 파일명(= {ContentVersion.Id}.{ext})을 바꾸지 마세요."
echo "       claim_upload_files.csv 의 UniqueKey__c 와 1:1 로 맞물립니다."
echo "════════════════════════════════════════════════════════════════"
echo

# -----------------------------------------------------------------------------
# 다음 단계 안내 — web SF Migration 화면에서 직접 처리
# -----------------------------------------------------------------------------

echo "다음 단계 — web SF Migration 화면 '클레임 이미지 적재' 섹션에서 (S3 업로드 완료 후):"
echo "  1) 클레임 이미지 적재 (target=ClaimImageUploadFile, s3KeyPrefix=$STAGE1_PREFIX)"
echo "     · bucket 은 폼에 프리필됨 (backend app.aws.s3.bucket)"
echo "     · 레거시 UploadFile 드롭다운과 분리된 전용 섹션"
echo "  2) Stage2 'UploadFile Parent Resolve' (클레임 row 도 record_sfid 조인으로 자동 연결)"
exit 0
