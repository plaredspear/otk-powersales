package com.otoki.powersales.domain.activity.promotion.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF `ProfessionalPromotionTeamMaster__c.ProfessionalPromotionTeam__c` picklist 정합 enum.
 *
 * - SF restricted = true (`<sorted>false</sorted>` — 정의된 순서를 그대로 UI 노출).
 * - enum 선언 순서가 SF picklist 정의 순서 (라면세일조 / 프레시세일조_냉동 / 프레시세일조_냉장 /
 *   프레시세일조_만두 / 카레세일조) 와 일치 — UI 드롭다운 순서로 그대로 사용된다.
 *
 * `CURRY_PROMOTION` 은 표시명이 '카레행사조' → '카레세일조' 로 변경됐다. 신규 저장/표시는
 * '카레세일조' 를 쓰되, 기존 DB 에 '카레행사조' 로 적재된 행을 그대로 읽어야 하므로
 * `legacyAliases` 에 이전 표시명을 등록해 read 시 동일 enum 으로 매핑한다 (write 는 항상 displayName).
 */
enum class ProfessionalPromotionTeamType(
    val displayName: String,
    /** 과거 표시명 등 이 enum 으로 매핑돼야 하는 대체 저장 문자열 (read 전용). */
    val legacyAliases: List<String> = emptyList()
) {
    RAMEN_SALE("라면세일조"),
    FRESH_SALE_FROZEN("프레시세일조_냉동"),
    FRESH_SALE_REFRIGERATED("프레시세일조_냉장"),
    FRESH_SALE_DUMPLING("프레시세일조_만두"),
    CURRY_PROMOTION("카레세일조", legacyAliases = listOf("카레행사조"));

    /** displayName + legacyAliases — 이 enum 으로 read 매핑돼야 하는 DB 저장 문자열 전체. */
    val storedValues: List<String>
        get() = listOf(displayName) + legacyAliases

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        /**
         * 전문행사조 미지정(해제) 상태의 표시명 — enum 값 아님.
         *
         * SF 레거시는 `ProfessionalPromotionTeam__c = '일반'` 문자열을 저장했지만,
         * 신규 시스템은 미지정을 null 로 표현한다. 검색 필터에서 "일반" 선택을 해석할 때 사용.
         */
        const val GENERAL_DISPLAY_NAME = "일반"

        /**
         * "미배정"으로 취급하는 DB 저장 문자열 목록.
         *
         * 신규 시스템의 미배정은 null 이지만, SF 레거시 데이터가 정규화 없이 적재되어
         * 컬럼에 '일반'·'해당없음' 문자열이 그대로 남아 있는 행이 존재한다
         * (converter 는 5개 정식 조가 아니면 null 로 변환하므로 화면에는 '일반'으로 표시됨).
         * "일반" 필터가 이 문자열 행까지 함께 조회하도록 하기 위한 목록.
         */
        val UNASSIGNED_LEGACY_VALUES: List<String> = listOf(GENERAL_DISPLAY_NAME, "해당없음")

        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProfessionalPromotionTeamType =
            entries.find { it.displayName == value || value in it.legacyAliases }
                ?: throw IllegalArgumentException("유효하지 않은 전문행사조 유형: $value")

        fun fromDisplayNameOrNull(value: String?): ProfessionalPromotionTeamType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value || value in it.legacyAliases }
        }

        /**
         * 필터로 넘어온 표시명 문자열 목록을, 각 enum 의 storedValues(displayName + legacyAliases) 로 확장한다.
         *
         * 표시명이 바뀐 유형(카레세일조 ← 카레행사조)을 필터에서 선택하면 신·구 저장 문자열을 모두
         * IN 매칭해야 하므로, 한글 문자열 컬럼(예: team_member_schedule.professional_promotion_team)을
         * 직접 필터링하는 경로에서 이 확장을 적용한다. enum 에 매칭되지 않는 값(예: '일반')은 그대로 보존한다.
         */
        fun expandStoredValues(displayNames: List<String>): List<String> =
            displayNames.flatMap { name ->
                entries.find { it.displayName == name || name in it.legacyAliases }
                    ?.storedValues
                    ?: listOf(name)
            }.distinct()
    }
}
