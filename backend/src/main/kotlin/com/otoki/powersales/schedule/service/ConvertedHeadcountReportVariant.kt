package com.otoki.powersales.schedule.service

/**
 * 거래처유형별 환산인원 현황 보고서 variant (Spec #847).
 *
 * SF Report 5변형의 filter 차이를 코드화. 각 variant 는 근무유형5 IN 값집합 / 빈값 포함 여부 /
 * 위탁농협 제외 여부 / 코스트센터(2팀) 코드 / 지점 기준(여사원 소속 vs 거래처) / 한글 보고서명 보유.
 *
 * | variant | SF | 근무유형5 | 빈값포함 | 위탁제외 | 코스트센터 | 지점기준 |
 * |---|---|---|---|---|---|---|
 * | PERMANENT_TEMP_ALL          | 1-1 | 상시,임시 | O | - | -     | 여사원소속 |
 * | PERMANENT_ONLY_EXCL_CONSIGN | 1-2 | 상시      | O | O | -     | 여사원소속 |
 * | TEMP_ALL                    | 1-4 | 임시      | - | - | -     | 여사원소속 |
 * | TEMP_ONLY_EXCL_CONSIGN      | 1-5 | 임시      | - | O | -     | 여사원소속 |
 * | TEAM2_PERMANENT_TEMP_ALL    | 2-1 | 상시,임시 | O | - | 4889  | 거래처    |
 */
enum class ConvertedHeadcountReportVariant(
    val reportName: String,
    val workingCategory5In: List<String>,
    val includeNullWc5: Boolean,
    val excludeConsignment: Boolean,
    val costCenterCode: String?,
    val useAccountBranch: Boolean,
) {
    PERMANENT_TEMP_ALL(
        reportName = "거래처유형별환산인원(상시임시전체)",
        workingCategory5In = listOf("상시", "임시"),
        includeNullWc5 = true,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = false,
    ),
    PERMANENT_ONLY_EXCL_CONSIGN(
        reportName = "거래처유형별환산인원(상시_위탁농협제외)",
        workingCategory5In = listOf("상시"),
        includeNullWc5 = true,
        excludeConsignment = true,
        costCenterCode = null,
        useAccountBranch = false,
    ),
    TEMP_ALL(
        reportName = "거래처유형별환산인원(임시전체)",
        workingCategory5In = listOf("임시"),
        includeNullWc5 = false,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = false,
    ),
    TEMP_ONLY_EXCL_CONSIGN(
        reportName = "거래처유형별환산인원(임시_위탁농협제외)",
        workingCategory5In = listOf("임시"),
        includeNullWc5 = false,
        excludeConsignment = true,
        costCenterCode = null,
        useAccountBranch = false,
    ),
    TEAM2_PERMANENT_TEMP_ALL(
        reportName = "2팀_거래처유형별환산인원(상시임시전체)",
        workingCategory5In = listOf("상시", "임시"),
        includeNullWc5 = true,
        excludeConsignment = false,
        costCenterCode = "4889",
        useAccountBranch = true,
    ),
}
