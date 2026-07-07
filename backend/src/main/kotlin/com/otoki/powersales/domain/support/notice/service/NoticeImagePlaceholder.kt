package com.otoki.powersales.domain.support.notice.service

/**
 * 공지 본문 인라인 이미지 placeholder 의 생성/파싱 단일 출처(SoT).
 *
 * 공지 본문(notice.contents) HTML 의 인라인 이미지는 만료 없는 placeholder `<img>` 태그로 영구 저장되고,
 * 조회 시점에 [NoticeService.getNoticeDetail] 가 `data-refid` 로 presigned URL 을 rewrite 한다.
 * placeholder 형식은 생성측(마이그레이션 치환)과 파싱측(조회 rewrite)이 정확히 맞물려야 하므로, 형식을
 * 본 object 한 곳에 모아 두 곳이 동일 정의를 참조하게 한다 (형식 변경 시 한 곳만 수정).
 *
 * placeholder 형식:
 *   <img src="notice-image://{refid}" data-refid="{refid}" [alt="{alt}"]>
 *   - src 의 `notice-image://` 커스텀 스킴 = http 아님 → rewrite 누락 시에도 잘못된 GET 안 나가고 깨진
 *     아이콘만 노출 (본문이 만료 URL 로 오염되는 것을 구조적으로 차단).
 *   - data-refid = 조회측 rewrite lookup 키 (= upload_file.sfid) + mobile cacheKey (권위 식별자).
 *
 * 외부 마이그레이션 스크립트 replace-notice-rta-urls.main.kts 도 동일 형식을 산출한다 (frozen 외부 스크립트라
 * 본 object 를 직접 참조하지 못하므로 형식을 수기 정합 — 변경 시 함께 갱신 의무).
 */
object NoticeImagePlaceholder {

    /** placeholder src 의 커스텀 스킴 prefix. */
    const val SCHEME = "notice-image://"

    /**
     * data-refid 속성을 가진 placeholder `<img>` 태그 전체. group(1) = refid.
     * 조회측 rewrite (NoticeService) 가 본 정규식으로 placeholder 를 찾아 src 만 presigned 로 교체한다.
     */
    val PLACEHOLDER_IMG_REGEX =
        Regex("""<img\b[^>]*\bdata-refid\s*=\s*"([^"]+)"[^>]*>""", RegexOption.IGNORE_CASE)

    /** `<img>` 태그 안의 src 속성 (presigned 로 교체 대상). */
    val SRC_ATTR_REGEX =
        Regex("""\bsrc\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE)

    /** 본문 `<img>` 태그의 src 값을 추출. group(1) = src 원문(presigned URL / placeholder scheme / 기타). */
    private val IMG_SRC_VALUE_REGEX =
        Regex("""<img\b[^>]*?\bsrc\s*=\s*"([^"]*)"[^>]*>""", RegexOption.IGNORE_CASE)

    /** private S3 key 의 세그먼트 prefix ("private/"). presigned URL path 에서 이 뒤가 uniqueKey. */
    private const val PRIVATE_PATH_SEGMENT = "private/"

    /**
     * base64 data URI 로 본문에 통째로 박혀 들어온 인라인 이미지 `<img src="data:image/...;base64,...">`.
     * group(1) = content-type(mime, 예: image/png), group(2) = base64 payload.
     *
     * 정상 인라인 업로드 경로([NoticeService.uploadNoticeInlineImage])는 본문에 placeholder 만 남기지만,
     * 웹 에디터 '붙여넣기' 등은 Quill 기본 동작으로 base64 를 본문에 그대로 삽입해 이 경로를 우회한다.
     * 그 결과 (1) DB contents 가 비대해지고 (2) 모바일이 http 아닌 src 를 렌더 못 해 이미지가 깨진다.
     * 저장 시점에 [NoticeService] 가 본 정규식으로 찾아 S3 업로드 + placeholder 치환하여 정규화한다.
     */
    val DATA_URI_IMG_REGEX =
        Regex(
            """<img\b[^>]*?\bsrc\s*=\s*"data:(image/[a-zA-Z0-9.+-]+);base64,([^"]*)"[^>]*>""",
            RegexOption.IGNORE_CASE
        )

    /** HTML attribute 값 이스케이프 (refid/alt 안의 " / & 안전 처리). */
    private fun escapeAttr(s: String): String = s.replace("&", "&amp;").replace("\"", "&quot;")

    /**
     * placeholder `<img>` 태그 생성. 마이그레이션 치환측이 본문의 원본 rtaImage `<img>` 를 본 결과로 통째 교체한다.
     * 조회측 [PLACEHOLDER_IMG_REGEX] 가 정확히 매칭하도록 data-refid 를 항상 포함한다.
     */
    fun build(refid: String, alt: String): String =
        "<img src=\"$SCHEME$refid\" data-refid=\"${escapeAttr(refid)}\"" +
            (if (alt.isNotEmpty()) " alt=\"${escapeAttr(alt)}\"" else "") + ">"

    /**
     * 본문 HTML 에서 placeholder 인라인 이미지의 refid 목록을 추출한다.
     * 신규 업로드 경로는 refid = upload_file.id (Long) 를 쓰므로, 공지 저장 시 본문이 참조하는
     * 임시 업로드 이미지의 parent_id 를 backfill 하기 위해 사용한다 (마이그레이션분의 sfid refid 는 Long 변환 실패로 자연 제외).
     */
    fun extractRefids(html: String): List<String> {
        if (!html.contains("data-refid")) return emptyList()
        return PLACEHOLDER_IMG_REGEX.findAll(html).map { it.groupValues[1] }.toList()
    }

    /**
     * 본문 HTML 의 모든 `<img src="...">` 에서 참조하는 private S3 uniqueKey 목록을 추출한다.
     *
     * 수정 화면은 상세조회가 rewrite 한 presigned URL(`https://.../private/{uniqueKey}?X-Amz-...`)을
     * 그대로 저장 본문에 담아 보낼 수 있다(웹 에디터가 data-refid 를 파싱 단계에서 버리기 때문).
     * presigned URL 은 만료·매번 재발급되는 임시값이라 식별자로 쓸 수 없으므로, URL path 에 내재된
     * 불변의 uniqueKey(= upload_file.unique_key)만 뽑아 "본문이 참조 중인 이미지" 판별에 쓴다.
     * 이로써 refid(data-refid) 소실 시에도 cleanup 이 본문 참조 이미지를 오삭제하지 않는다.
     *
     * 매칭 규칙: src 를 `?` 기준으로 잘라 쿼리스트링(만료 서명)을 제거한 뒤, path 에서 "private/" 이후를
     * uniqueKey 로 취한다. "private/" 를 포함하지 않는 src(placeholder scheme, 외부 URL 등)는 제외한다.
     */
    fun extractUniqueKeys(html: String): List<String> {
        if (!html.contains(PRIVATE_PATH_SEGMENT)) return emptyList()
        return IMG_SRC_VALUE_REGEX.findAll(html)
            .mapNotNull { match -> uniqueKeyFromSrc(match.groupValues[1]) }
            .toList()
    }

    /**
     * 단일 img src 값에서 uniqueKey 를 해석한다. "private/" 미포함 시 null.
     * URL 인코딩된 경로 세그먼트는 uniqueKey(uploads/notice/yyyy/mm/dd/uuid.ext)에 인코딩 대상 문자가
     * 없으므로 별도 디코딩 없이 그대로 비교 가능하다(영숫자/`/`/`.`/`-` 로만 구성).
     */
    private fun uniqueKeyFromSrc(src: String): String? {
        val withoutQuery = src.substringBefore('?')
        val idx = withoutQuery.indexOf(PRIVATE_PATH_SEGMENT)
        if (idx < 0) return null
        return withoutQuery.substring(idx + PRIVATE_PATH_SEGMENT.length).takeIf { it.isNotBlank() }
    }
}
