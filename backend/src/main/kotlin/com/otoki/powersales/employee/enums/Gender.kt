package com.otoki.powersales.employee.enums

/**
 * 성별 enum.
 *
 * - DB 저장 형식: enum name (`MALE` / `FEMALE`) — `@Enumerated(EnumType.STRING)`
 * - 화면 표시: [displayName] ("남" / "여")
 * - SAP 페이로드 매핑: [sapCode] ("1" → MALE, "2" → FEMALE) via [fromSapCode]
 */
enum class Gender(val displayName: String, val sapCode: String) {
    MALE("남", "1"),
    FEMALE("여", "2");

    companion object {
        fun fromSapCode(code: String?): Gender? = when (code) {
            "1" -> MALE
            "2" -> FEMALE
            else -> null
        }
    }
}
