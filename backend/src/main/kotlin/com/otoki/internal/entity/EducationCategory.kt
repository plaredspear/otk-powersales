package com.otoki.internal.entity

/**
 * 교육 카테고리 Enum
 */
enum class EducationCategory(
    val displayName: String
) {
    /**
     * 시식 매뉴얼
     */
    TASTING_MANUAL("시식 매뉴얼"),

    /**
     * CS/안전
     */
    CS_SAFETY("CS/안전"),

    /**
     * 교육 평가
     */
    EVALUATION("교육 평가"),

    /**
     * 신제품 소개
     */
    NEW_PRODUCT("신제품 소개");

    companion object {
        /**
         * 문자열을 EducationCategory enum으로 변환
         * @throws IllegalArgumentException 유효하지 않은 카테고리 문자열인 경우
         */
        fun fromString(category: String): EducationCategory {
            return entries.find { it.name == category }
                ?: throw IllegalArgumentException("Invalid category: $category")
        }
    }
}
