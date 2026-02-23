/*
package com.otoki.internal.entity

// V1은 String edu_code 사용 — Enum 불필요 (Spec 73)

enum class EducationCategory(
    val displayName: String
) {
    TASTING_MANUAL("시식 매뉴얼"),
    CS_SAFETY("CS/안전"),
    EVALUATION("교육 평가"),
    NEW_PRODUCT("신제품 소개");

    companion object {
        fun fromString(category: String): EducationCategory {
            return entries.find { it.name == category }
                ?: throw IllegalArgumentException("Invalid category: $category")
        }
    }
}
*/
