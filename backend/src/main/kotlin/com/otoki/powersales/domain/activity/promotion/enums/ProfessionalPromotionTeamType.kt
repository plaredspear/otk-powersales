package com.otoki.powersales.domain.activity.promotion.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF `ProfessionalPromotionTeamMaster__c.ProfessionalPromotionTeam__c` picklist 정합 enum.
 *
 * - SF restricted = true (`<sorted>false</sorted>` — 정의된 순서를 그대로 UI 노출).
 * - enum 선언 순서가 SF picklist 정의 순서 (라면세일조 / 프레시세일조_냉동 / 프레시세일조_냉장 /
 *   프레시세일조_만두 / 카레행사조) 와 일치 — UI 드롭다운 순서로 그대로 사용된다.
 */
enum class ProfessionalPromotionTeamType(
    val displayName: String
) {
    RAMEN_SALE("라면세일조"),
    FRESH_SALE_FROZEN("프레시세일조_냉동"),
    FRESH_SALE_REFRIGERATED("프레시세일조_냉장"),
    FRESH_SALE_DUMPLING("프레시세일조_만두"),
    CURRY_PROMOTION("카레행사조");

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
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 전문행사조 유형: $value")

        fun fromDisplayNameOrNull(value: String?): ProfessionalPromotionTeamType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
