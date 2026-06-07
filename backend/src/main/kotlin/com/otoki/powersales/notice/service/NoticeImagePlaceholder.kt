package com.otoki.powersales.notice.service

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

    /** HTML attribute 값 이스케이프 (refid/alt 안의 " / & 안전 처리). */
    private fun escapeAttr(s: String): String = s.replace("&", "&amp;").replace("\"", "&quot;")

    /**
     * placeholder `<img>` 태그 생성. 마이그레이션 치환측이 본문의 원본 rtaImage `<img>` 를 본 결과로 통째 교체한다.
     * 조회측 [PLACEHOLDER_IMG_REGEX] 가 정확히 매칭하도록 data-refid 를 항상 포함한다.
     */
    fun build(refid: String, alt: String): String =
        "<img src=\"$SCHEME$refid\" data-refid=\"${escapeAttr(refid)}\"" +
            (if (alt.isNotEmpty()) " alt=\"${escapeAttr(alt)}\"" else "") + ">"
}
