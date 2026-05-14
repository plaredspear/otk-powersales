package com.otoki.powersales.sales.enums

/**
 * SF picklist `SalesYear__c` 옵션 enum.
 * SF 원본 옵션값 (`2019` ~ `2030`, 활성 12개) 을 보존한다.
 */
enum class SalesYear(val value: String) {
    Y2019("2019"),
    Y2020("2020"),
    Y2021("2021"),
    Y2022("2022"),
    Y2023("2023"),
    Y2024("2024"),
    Y2025("2025"),
    Y2026("2026"),
    Y2027("2027"),
    Y2028("2028"),
    Y2029("2029"),
    Y2030("2030");

    companion object {
        fun fromValueOrNull(value: String?): SalesYear? =
            value?.let { v -> entries.firstOrNull { it.value == v } }
    }
}
