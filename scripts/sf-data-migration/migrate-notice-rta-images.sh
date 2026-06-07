#!/usr/bin/env bash
#
# 공지사항 본문 RTA 인라인 이미지 마이그레이션 오케스트레이션 (반복 실행 가능, 사용자 수동 실행)
#
# 배경:
#   공지(DKRetail__Notice__c) 본문(rich text, DKRetail__Contents__c)의 인라인 이미지는 레거시에서 SF
#   표준 rich text area 의 인라인 blob 으로 저장되어 본문 HTML 에 rtaImage 서블릿 URL
#   (https://<file-domain>/servlet/rtaImage?eid=...&feoid=...&refid=0EM...)로 박힌다. 이미지 바이트는
#   ContentDocumentLink / ContentVersion / UploadFile__c 어디에도 행이 없어 SOQL 로 조회 불가하고,
#   rtaImage 서블릿 HTTP GET 이 유일한 다운로드 경로다. 신규 시스템은 본문 URL 이 SF org 인증에 묶여
#   깨지므로, 이미지를 직접 다운로드 → S3 재업로드 → upload_file 적재 → 본문 URL 을 신규 public URL 로
#   영구 치환한다. cut-over 전까지 반복 실행 가능하도록 전 단계를 스크립트화한다 (각 단계 멱등 / --skip-*).
#
#   참고: 본문에 rtaImage 가 없고 첨부 위젯 이미지만 있는 공지(예: notice 2193)는 이미 upload_file 에
#   적재되어 있어 본 스크립트 대상이 아니다 (scan 에서 자동 제외 — 본문에 rtaImage 가 있는 공지만 추출).
#
# 역할 분담 (클레임 이미지 스크립트와 동일):
#   본 스크립트는 **로컬 산출물 준비 + 콘솔 업로드 경로 안내 + 본문 치환 SQL 안내**까지 담당한다.
#   - S3 업로드     : AWS 콘솔에서 사용자가 직접 (스크립트는 prefix 경로만 안내).
#   - Stage1/Stage2 : web 의 SF Migration 화면에서 처리 (target=NoticeImageUploadFile).
#   - 본문 URL 치환 : replace-notice-rta-urls.main.kts (DB UPDATE, 멱등) — 적재 완료 후 사용자 실행.
#
# 산출물 위치 (<out> = input/notice-images/):
#   기존 input/upload_files.csv (레거시 UploadFile__c) / claim_upload_files.csv 와 분리 격리.
#
# 파이프라인 (단계):
#   1) scan       sf data query (Id, Contents__c) → 본문 파싱 → <out>/notice-rta-scan.csv
#                 (한 행 = 본문 안 rtaImage <img> 1개: notice_sfid, refid, source_url, alt_name)
#   2) download   scan CSV 행별 rtaImage 서블릿 GET (access token) → <out>/images/{refid}.{ext} (증분)
#   3) build-csv  build-notice-image-upload-files.main.kts → <out>/notice_image_upload_files.csv
#   --- 이후 S3 업로드(콘솔) → web Stage1/Stage2 → 본문 치환(kts) 안내만 출력 ---
#
# 사전 준비:
#   - sf CLI 인증:   sf org login web --alias <alias>   (또는 sf org list 로 확인)
#
# 사용법:
#   ./migrate-notice-rta-images.sh --org <alias>
#       → 로컬 준비 + 콘솔/Stage/치환 안내
#       → Stage1 CSV prefix 는 sf-migration/notice-images 고정 (--stage1-prefix 로 override)
#   ./migrate-notice-rta-images.sh ... --skip-scan --skip-download   # 변환만 재시도
#   ./migrate-notice-rta-images.sh ... --count-only                  # 추출 대상(본문 rtaImage) 건수만
#   ./migrate-notice-rta-images.sh ... --limit 50                    # 샘플 50개 공지만 scan
#   ./migrate-notice-rta-images.sh ... --parallel 8                  # 다운로드 8개 동시
#   ./migrate-notice-rta-images.sh ... --sid <쿠키값>                # Bearer 실패 시 sid 쿠키 fallback
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
#   조회 시 PublicUrlResolver 가 prefix(S3_PUBLIC_URL_PREFIX, .../public/ 로 끝남)에 unique_key 를
#   이어붙여 완전 URL 을 만든다. public/ 을 넣으면 .../public/public/uploads/... 로 중복된다.
#   따라서 unique_key = uploads/notice/migrated/{refid}.{ext}, S3 실제 객체 key = public/ + unique_key.
IMAGE_PREFIX="uploads/notice/migrated"
# S3 객체 key 는 public/ 하위 (anonymous GET). 콘솔 업로드 대상 = PUBLIC_SEGMENT + "/" + IMAGE_PREFIX.
PUBLIC_SEGMENT="public"
# 이미지 확장자 화이트리스트 (Content-Type 으로 판정 못한 경우 fallback 검증용).
IMAGE_EXTS="jpg,jpeg,png,gif,bmp,webp,heic,heif"
# Stage1 CSV prefix — 공지 본문 이미지 전용 고정값 (레거시/클레임 CSV 와 분리). web 카드 기본값과 정합.
STAGE1_PREFIX="sf-migration/notice-images"
# 산출물 전용 하위폴더 (input/upload_files.csv / claim 산출물과 분리 격리).
OUT_DIR="$SCRIPT_DIR/input/notice-images"
# Bearer 인증이 file 도메인에서 실패할 때 사용자가 브라우저에서 추출한 sid 쿠키 (선택).
SID_COOKIE=""

SKIP_SCAN=0
SKIP_DOWNLOAD=0
SKIP_BUILD_CSV=0
COUNT_ONLY=0
LIMIT=""
PARALLEL=1

usage() {
    sed -n '2,52p' "$0"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --org)           SF_ORG="$2"; shift 2 ;;
        --api-version)   SF_API_VERSION="$2"; shift 2 ;;
        --image-prefix)  IMAGE_PREFIX="${2%/}"; shift 2 ;;
        --image-exts)    IMAGE_EXTS="$2"; shift 2 ;;
        --stage1-prefix) STAGE1_PREFIX="${2%/}"; shift 2 ;;
        --out-dir)       OUT_DIR="$2"; shift 2 ;;
        --sid)           SID_COOKIE="$2"; shift 2 ;;
        --skip-scan)        SKIP_SCAN=1; shift ;;
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
# 가드 — IMAGE_PREFIX 는 public/ 으로 시작할 수 없다 (PublicUrlResolver prefix 가 .../public/ 로 끝나
# 조회 시 .../public/public/uploads/... 중복 발생). 클레임 스크립트 가드와 동일. (과거 사고 재발 방지)
prefix_lc="$(printf '%s' "${IMAGE_PREFIX#/}" | tr '[:upper:]' '[:lower:]')"
if [[ "$prefix_lc" == public/* ]]; then
    echo "[error] --image-prefix 는 public/ 으로 시작할 수 없습니다: $IMAGE_PREFIX" >&2
    echo "        PublicUrlResolver prefix 가 .../public/ 로 끝나 중복됩니다." >&2
    echo "        예: uploads/notice/migrated (public 없이). S3 객체 폴더는 PUBLIC_SEGMENT 가 자동 부여." >&2
    exit 1
fi
if [[ ! "$PARALLEL" =~ ^[0-9]+$ || "$PARALLEL" -lt 1 ]]; then
    echo "[error] --parallel 은 1 이상의 정수여야 함: $PARALLEL" >&2; exit 1
fi

SF_ORG_ARGS=()
if [[ -n "$SF_ORG" ]]; then
    SF_ORG_ARGS=(--target-org "$SF_ORG")
fi

SCAN_CSV="$OUT_DIR/notice-rta-scan.csv"
FAILED_CSV="$OUT_DIR/notice_rta_failed.csv"
IMG_DIR="$OUT_DIR/images"
# 파일명은 backend Stage1 NoticeImageUploadFile 타겟 csvFileName 과 정합해야 한다.
UPLOAD_CSV="$OUT_DIR/notice_image_upload_files.csv"

mkdir -p "$OUT_DIR" "$IMG_DIR"

echo "[info] out dir          : $OUT_DIR"
echo "[info] unique_key prefix : $IMAGE_PREFIX  (DB 적재값, public/ 없음)"
echo "[info] s3 객체 폴더      : $PUBLIC_SEGMENT/$IMAGE_PREFIX/  (콘솔 업로드 대상)"
echo "[info] stage1 prefix     : $STAGE1_PREFIX"
echo "[info] sf org            : ${SF_ORG:-(default)}"
echo "[info] api version       : $SF_API_VERSION"
echo "[info] s3 upload         : 콘솔 수동 (안내만)"
echo "[info] parallel          : $PARALLEL"
echo "[info] limit             : ${LIMIT:-(전체)}"
echo "[info] sid fallback      : ${SID_COOKIE:+설정됨}"
echo

# 본문에 rtaImage 가 박힌 공지만 대상. (첨부 위젯 이미지만 있는 공지는 이미 upload_file 에 적재됨 — 제외.)
NOTICE_WHERE="WHERE DKRetail__Contents__c LIKE '%rtaImage%'"
NOTICE_SELECT="SELECT Id, DKRetail__Contents__c FROM DKRetail__Notice__c $NOTICE_WHERE"
if [[ -n "$LIMIT" ]]; then
    NOTICE_SELECT="$NOTICE_SELECT LIMIT $LIMIT"
fi

# -----------------------------------------------------------------------------
# 추출 대상 건수 미리보기 (항상 출력)
# -----------------------------------------------------------------------------

echo "[count] 본문 rtaImage 포함 공지 건수 확인 중..."
total_target="$(sf data query \
    --query "SELECT COUNT() FROM DKRetail__Notice__c $NOTICE_WHERE" \
    --result-format json \
    --api-version "$SF_API_VERSION" \
    ${SF_ORG_ARGS[@]+"${SF_ORG_ARGS[@]}"} | python3 -c 'import sys,json; print(json.load(sys.stdin).get("result",{}).get("totalSize",""))')"
echo "[count] 본문에 rtaImage 가 박힌 공지: ${total_target} 건 (한 공지에 여러 이미지 가능)"
if [[ -n "$LIMIT" ]]; then
    echo "[count] 이번 실행 scan 대상(--limit): $LIMIT 건 (전체 중 일부)"
fi
echo

if [[ "$COUNT_ONLY" -eq 1 ]]; then
    echo "[done] --count-only — 건수만 확인하고 종료."
    exit 0
fi

# -----------------------------------------------------------------------------
# 1) scan — 공지 본문 파싱 → rtaImage <img> 튜플 CSV
# -----------------------------------------------------------------------------
# sf data query (Id, Contents__c) 결과 CSV 를 python 으로 파싱한다. 본문 HTML 안 <img src="...rtaImage...">
# 마다 한 행(notice_sfid, refid, source_url, alt_name)을 출력한다. source_url 은 본문에 박힌 원본 문자열
# (&amp; 인코딩 형태 그대로 — 치환 단계 매칭 키). refid/eid 추출은 &amp; → & 디코딩 후 파라미터 파싱.

if [[ "$SKIP_SCAN" -eq 1 ]]; then
    echo "[skip] scan"
    if [[ ! -f "$SCAN_CSV" ]]; then
        echo "[error] --skip-scan 인데 scan CSV 없음: $SCAN_CSV" >&2; exit 1
    fi
else
    echo "[step 1/3] scan 공지 본문 → $SCAN_CSV${LIMIT:+ (LIMIT $LIMIT)}"
    raw_csv="$OUT_DIR/notice-contents-raw.csv"
    sf data query \
        --query "$NOTICE_SELECT" \
        --result-format csv \
        --api-version "$SF_API_VERSION" \
        ${SF_ORG_ARGS[@]+"${SF_ORG_ARGS[@]}"} > "$raw_csv"

    SCAN_CSV="$SCAN_CSV" python3 - "$raw_csv" <<'PYEOF'
import csv, html, os, re, sys

raw_path = sys.argv[1]
out_path = os.environ["SCAN_CSV"]

# <img ...> 태그 + 그 안의 rtaImage src / alt 추출.
img_re = re.compile(r'<img\b[^>]*>', re.IGNORECASE)
src_re = re.compile(r'\bsrc\s*=\s*"([^"]*rtaImage[^"]*)"', re.IGNORECASE)
alt_re = re.compile(r'\balt\s*=\s*"([^"]*)"', re.IGNORECASE)

def param(url, key):
    # url 은 &amp; 인코딩 형태일 수 있으므로 디코딩 후 파싱.
    decoded = html.unescape(url)
    m = re.search(r'[?&]' + re.escape(key) + r'=([^&]+)', decoded)
    return m.group(1) if m else ""

notices = images = 0
with open(raw_path, newline="") as f, open(out_path, "w", newline="") as out:
    reader = csv.DictReader(f)
    writer = csv.writer(out)
    writer.writerow(["notice_sfid", "refid", "eid", "source_url", "alt_name"])
    for row in reader:
        sfid = (row.get("Id") or "").strip()
        contents = row.get("DKRetail__Contents__c") or ""
        if not sfid or "rtaImage" not in contents:
            continue
        notices += 1
        for tag in img_re.findall(contents):
            sm = src_re.search(tag)
            if not sm:
                continue
            src = sm.group(1)            # 본문에 박힌 원본 문자열 (&amp; 형태 그대로 — 치환 매칭 키)
            refid = param(src, "refid")
            eid = param(src, "eid")
            if not refid:
                continue
            am = alt_re.search(tag)
            alt = html.unescape(am.group(1)) if am else ""
            writer.writerow([sfid, refid, eid, src, alt])
            images += 1

sys.stderr.write("notices=%d images=%d\n" % (notices, images))
PYEOF

    scan_rows=$(($(wc -l < "$SCAN_CSV") - 1))
    echo "[ok] scan rows (rtaImage <img>): $scan_rows"
fi
echo

# -----------------------------------------------------------------------------
# 2) download — rtaImage 서블릿 GET (access token, 증분, 병렬)
# -----------------------------------------------------------------------------
# rtaImage 는 REST sobject 엔드포인트가 아니라 *.file.force.com/servlet/rtaImage 서블릿이라
# sf api request 로 못 받는다. sf org display 로 accessToken + instanceUrl 을 추출해 curl Bearer 로
# GET 한다. file 도메인이 Bearer 를 거부하면 사용자 브라우저 sid 쿠키(--sid) fallback.
# 응답 분류: 2xx + image/* = 성공 / 2xx + text/html = 세션만료(실패) / 4xx,5xx = 다운로드 실패.
# 실패분은 notice_rta_failed.csv 에 누적 → --skip-download 없이 재실행 시 성공분 skip + 실패분만 재시도.

if [[ "$SKIP_DOWNLOAD" -eq 1 ]]; then
    echo "[skip] download"
else
    echo "[step 2/3] download rtaImage (parallel=$PARALLEL) → $IMG_DIR (증분)"

    # access token + instance url 추출 (sf 실행 주체는 사용자 — 자발 호출 정책 위배 아님).
    org_json="$(sf org display --verbose --json ${SF_ORG_ARGS[@]+"${SF_ORG_ARGS[@]}"})"
    ACCESS_TOKEN="$(printf '%s' "$org_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["result"].get("accessToken",""))')"
    INSTANCE_URL="$(printf '%s' "$org_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["result"].get("instanceUrl",""))')"
    if [[ -z "$ACCESS_TOKEN" || -z "$INSTANCE_URL" ]]; then
        echo "[error] sf org display 에서 accessToken/instanceUrl 추출 실패 — sf 인증 상태 확인" >&2
        exit 1
    fi
    echo "[info] instance url   : $INSTANCE_URL"

    export IMG_DIR ACCESS_TOKEN INSTANCE_URL SID_COOKIE IMAGE_EXTS FAILED_CSV

    : > "$FAILED_CSV"
    echo "refid,reason" >> "$FAILED_CSV"

    before=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
    SECONDS=0

    # scan CSV (헤더 제외) → 워커. 워커: 이미 받은 파일 skip → curl GET → Content-Type 으로 확장자/성공 판정.
    tail -n +2 "$SCAN_CSV" | python3 -c '
import csv, sys
w = csv.writer(sys.stdout)
for row in csv.reader(sys.stdin):
    if len(row) >= 4:
        # refid \t source_url(원본 문자열) → 워커로 전달 (탭 구분, source_url 은 디코딩해 GET).
        sys.stdout.write(row[1] + "\t" + row[3] + "\n")
' | sort -u | xargs -P "$PARALLEL" -I {} bash -c '
        line="$1"
        refid="${line%%	*}"
        src="${line#*	}"
        [ -z "$refid" ] && exit 0
        # 이미 받은 파일 있으면 skip (확장자 무관 — refid.* 존재 검사).
        if ls "$IMG_DIR/$refid".* >/dev/null 2>&1; then exit 0; fi
        # source_url 의 &amp; → & 디코딩 + 절대 URL 화 (instanceUrl 의 file 도메인으로).
        url=$(printf "%s" "$src" | python3 -c "import sys,html; u=html.unescape(sys.stdin.read().strip()); print(u)")
        case "$url" in
            http*) full="$url" ;;
            /*)    full="${INSTANCE_URL%/}$url" ;;
            *)     full="${INSTANCE_URL%/}/$url" ;;
        esac
        tmp="$IMG_DIR/.$refid.tmp"
        # Bearer 우선, sid 쿠키 있으면 함께 전송.
        hdr=(-H "Authorization: Bearer $ACCESS_TOKEN")
        [ -n "$SID_COOKIE" ] && hdr+=(--cookie "sid=$SID_COOKIE")
        http_code=$(curl -sS -L -o "$tmp" -w "%{http_code}" -D "$tmp.hdr" "${hdr[@]}" "$full" 2>/dev/null || echo 000)
        ctype=$(grep -i "^content-type:" "$tmp.hdr" 2>/dev/null | tail -1 | tr -d "\r" | sed "s/.*: *//;s/;.*//" | tr "[:upper:]" "[:lower:]")
        rm -f "$tmp.hdr"
        if [ "$http_code" = "200" ] && printf "%s" "$ctype" | grep -q "^image/"; then
            ext="${ctype#image/}"; [ "$ext" = "jpeg" ] && ext="jpg"
            mv "$tmp" "$IMG_DIR/$refid.$ext"
        else
            rm -f "$tmp"
            reason="http=$http_code ctype=$ctype"
            if [ "$http_code" = "200" ]; then reason="session-expired ($reason)"; fi
            printf "%s,%s\n" "$refid" "$reason" >> "$FAILED_CSV"
        fi
    ' _ {}

    after=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')
    dl=$((after - before))
    fail=$(($(wc -l < "$FAILED_CSV") - 1))
    echo "[ok] downloaded(new)=$dl  total-on-disk=$after  elapsed=${SECONDS}s  parallel=$PARALLEL"
    if [[ "$fail" -gt 0 ]]; then
        echo "[warn] 실패 $fail 건 — $FAILED_CSV 참조. 재실행하면 실패분만 다시 받습니다 (세션만료면 --sid 추가)." >&2
    fi
fi
echo

# -----------------------------------------------------------------------------
# 3) build-csv — scan CSV + 다운로드 결과 → notice_image_upload_files.csv
# -----------------------------------------------------------------------------

if [[ "$SKIP_BUILD_CSV" -eq 1 ]]; then
    echo "[skip] build-csv"
    if [[ ! -f "$UPLOAD_CSV" ]]; then
        echo "[error] --skip-build-csv 인데 notice_image_upload_files.csv 없음: $UPLOAD_CSV" >&2; exit 1
    fi
else
    echo "[step 3/3] build notice_image_upload_files.csv → $UPLOAD_CSV"
    kotlinc -script "$SCRIPT_DIR/build-notice-image-upload-files.main.kts" -- \
        --scan-csv "$SCAN_CSV" \
        --img-dir "$IMG_DIR" \
        --out "$UPLOAD_CSV" \
        --image-prefix "$IMAGE_PREFIX"
fi
echo

img_count=$(find "$IMG_DIR" -type f 2>/dev/null | wc -l | tr -d ' ')

# -----------------------------------------------------------------------------
# S3 업로드 / Stage / 본문 치환 안내
# -----------------------------------------------------------------------------

echo "════════════════════════════════════════════════════════════════"
echo " 로컬 산출물 준비 완료 — 이후 절차 안내"
echo "════════════════════════════════════════════════════════════════"
echo
echo " ① 공지 본문 이미지 ($img_count 개) → AWS 콘솔 업로드"
echo "    로컬 : $IMG_DIR/   (폴더 안 파일들)"
echo "    대상 : <운영 S3 버킷>/$PUBLIC_SEGMENT/$IMAGE_PREFIX/"
echo "           ※ unique_key 는 public/ 없이 '$IMAGE_PREFIX/...' 이고, S3 객체 key 는 public/ 하위."
echo
echo " ② Stage1 적재용 CSV → AWS 콘솔 업로드"
echo "    로컬 : $UPLOAD_CSV"
echo "    대상 : <운영 S3 버킷>/$STAGE1_PREFIX/notice_image_upload_files.csv"
echo
echo " ③ web SF Migration 화면 '공지 본문 이미지 적재' 섹션 (S3 업로드 완료 후):"
echo "    · target=NoticeImageUploadFile, s3KeyPrefix=$STAGE1_PREFIX (폼 기본값)"
echo "    · 이어서 Stage2 'UploadFile Parent Resolve' (record_sfid 조인으로 공지 자동 연결)"
echo
echo " ④ 본문 HTML rtaImage URL → 신규 public URL 치환 (Stage1/Stage2 완료 후):"
echo "    # 사전: db.properties 채우기 (cp db.properties.template db.properties)"
echo "    kotlinc -script $SCRIPT_DIR/replace-notice-rta-urls.main.kts -- \\"
echo "        --scan-csv $SCAN_CSV --upload-csv $UPLOAD_CSV \\"
echo "        --image-prefix $IMAGE_PREFIX \\"
echo "        --public-url-prefix <S3_PUBLIC_URL_PREFIX> [--apply]"
echo "    (--apply 없으면 dry-run — 변경 대상만 출력. 멱등: 이미 치환된 본문은 skip.)"
echo
echo " 주의: 이미지 파일명(= {refid}.{ext})을 바꾸지 마세요."
echo "       notice_image_upload_files.csv 의 UniqueKey__c 와 1:1 로 맞물립니다."
echo "════════════════════════════════════════════════════════════════"
exit 0
