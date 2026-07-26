package com.otoki.powersales.domain.support.education.enums

/**
 * 교육 카테고리 Enum
 *
 * `education_post.edu_code` 에 저장되는 분류 코드의 단일 진실 공급원이다.
 *
 * 원래 이 값은 `education_code` 테이블(레거시 `education_code_mng`)에서 읽었지만,
 * - 카테고리를 CRUD 하는 관리 기능이 없고,
 * - 모바일 교육 메인의 타일 자체가 앱 내 enum(EducationCategory)이라 DB 에만 코드를 추가해도
 *   앱에는 보이지 않는다(= DB 로 관리해서 얻는 이점이 없다).
 *
 * 그래서 코드/표시명을 백엔드 코드로 끌어와, DB 행이 없어서 조회가 400 으로 떨어지던
 * 불일치를 없앴다. 카테고리 추가는 이 enum + 앱 enum 을 함께 고치는 것으로 끝난다.
 * `education_code` 테이블은 레거시 이관 데이터로만 남는다.
 *
 * 선언 순서 = 어드민 카테고리 선택 목록의 노출 순서.
 */
enum class EducationCategoryCode(
    val code: String,
    val displayName: String
) {
    TASTING_MANUAL("c00001", "시식 매뉴얼"),
    SAFETY("c00002", "안전교육"),
    SALES_EDUCATION("c00003", "영업 교육"),
    APP_MANUAL("c00005", "APP 매뉴얼"),
    SURVEY("c00004", "설문조사");

    companion object {
        fun fromCodeOrNull(code: String?): EducationCategoryCode? =
            entries.find { it.code == code }

        fun exists(code: String): Boolean = fromCodeOrNull(code) != null

        /** 코드에 해당하는 표시명. 미등록 코드(레거시 잔존분)는 빈 문자열로 둔다. */
        fun displayNameOf(code: String?): String = fromCodeOrNull(code)?.displayName ?: ""
    }
}
