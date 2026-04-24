package com.otoki.powersales.sap.entity

/**
 * SAP 근태 유형 코드 enum
 * 비즈니스 로직 판별 전용 — DB 저장은 기존 String 유지
 */
enum class AttendType(
    val code: String,
    val displayName: String,
    val isAnnualLeave: Boolean
) {
    UNDER_ONE_YEAR("10", "1년미만연차", true),
    ANNUAL_LEAVE("14", "연차", true),
    MID_YEAR_LEAVE("20", "연중휴가", true),
    FAMILY_EVENT("90", "경조", true),
    BIRTHDAY_LEAVE("120", "생휴", true),
    COMBINED_LEAVE("133", "연중&하기&하계", true);

    companion object {
        val ANNUAL_LEAVE_CODES: Set<String> = entries
            .filter { it.isAnnualLeave }
            .map { it.code }
            .toSet()

        private val CODE_MAP: Map<String, AttendType> = entries.associateBy { it.code }

        fun fromCode(code: String): AttendType? = CODE_MAP[code]
    }
}
