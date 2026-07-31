package com.otoki.powersales.domain.support.notice.enums

/**
 * 공지사항 분류 Enum
 * - dbValue: DB 컬럼(dkretail__category__c)에 저장되는 Salesforce Picklist 원본값
 * - apiCode: API 요청/응답에 사용되는 코드
 * - displayName: 화면 표시용 한글명
 */
enum class NoticeCategory(
    val displayName: String,
    val dbValue: String,
    val apiCode: String
) {
    COMPANY("회사공지", "회사공지", "COMPANY"),
    BRANCH("지점공지", "영업부/지점공지", "BRANCH"),
    EDUCATION("교육", "교육", "EDUCATION");

    /**
     * 홈 화면 공지 카드 표시용 라벨.
     *
     * 레거시(home.jsp)는 홈 공지 카드를 2분류로 묶어 표시했다.
     * 회사공지·교육은 "전체 공지", 영업부/지점공지는 "지점 공지"로 노출한다.
     */
    val homeDisplayName: String
        get() = when (this) {
            BRANCH -> "지점 공지"
            COMPANY, EDUCATION -> "전체 공지"
        }

    companion object {
        /**
         * 화면에서 고를 수 있는 분류 — 작성 폼 선택지와 목록 조회 필터가 공유한다.
         *
         * **교육(EDUCATION)은 제외**한다. 레거시 Heroku 는 2020-11-04 에 교육을 공지에서 분리해
         * 별도 화면/테이블로 뺐고(`CommunityController#noticeSelectList` 주석 "교육검색 삭제해야함(따로뺌)"
         * + `eduSelectList` → education 테이블), 공지 목록 쿼리(`communityMapper.xml#selectNotice`)의
         * 분류 분기도 회사공지/지점공지 둘뿐이다. 신규 시스템도 교육을 별도 도메인(`/education`)으로
         * 구현했으므로 공지 분류의 교육 축은 쓰지 않는다. SF 데이터 마이그레이션 대상에서도 공지사항이
         * 제외되어 교육 분류 공지가 적재될 경로가 없다.
         *
         * enum 값 자체는 남겨 둔다 — DB 에 '교육' 값이 들어오더라도 [NoticeCategoryConverter] 가
         * 읽을 수 있어야 하고, 홈 카드 묶음([homeDisplayName])도 그 값을 다룬다.
         */
        val SELECTABLE: List<NoticeCategory> = entries.filter { it != EDUCATION }

        fun fromApiCode(code: String): NoticeCategory {
            return entries.find { it.apiCode == code }
                ?: throw IllegalArgumentException("Invalid category: $code")
        }

        fun fromDbValue(value: String): NoticeCategory {
            return entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Invalid db value: $value")
        }
    }
}
