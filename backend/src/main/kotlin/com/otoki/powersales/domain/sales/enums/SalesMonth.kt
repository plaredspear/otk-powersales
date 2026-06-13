package com.otoki.powersales.domain.sales.enums

/**
 * SF picklist `SalesMonth__c` 옵션 enum.
 * SF 원본 옵션값 (`01` ~ `12`) 을 보존한다.
 */
enum class SalesMonth(val value: String) {
    M01("01"),
    M02("02"),
    M03("03"),
    M04("04"),
    M05("05"),
    M06("06"),
    M07("07"),
    M08("08"),
    M09("09"),
    M10("10"),
    M11("11"),
    M12("12");

    companion object {
        fun fromValueOrNull(value: String?): SalesMonth? =
            value?.let { v -> entries.firstOrNull { it.value == v } }
    }
}
