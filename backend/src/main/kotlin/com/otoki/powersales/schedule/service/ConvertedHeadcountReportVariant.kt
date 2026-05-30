package com.otoki.powersales.schedule.service

/**
 * 거래처유형별 환산인원 현황 보고서 variant (Spec #847 — 거래처유형 5종 + 대리점/대형마트 5종).
 *
 * SF Report 변형의 filter/grouping 차이를 코드화. 각 variant 는 근무유형5 IN 값집합 / 빈값 포함 여부 /
 * 위탁농협 제외 여부 / 코스트센터(2팀) 코드 / 지점 기준(여사원 소속 vs 거래처) /
 * 구분(거래처유형) 필터 / 근무유형3 컬럼 포함 여부 / 한글 보고서명 보유.
 *
 * | variant | SF | 구분필터 | 근무유형5 | 빈값포함 | 위탁제외 | 코스트센터 | 지점기준 | 근무유형3 |
 * |---|---|---|---|---|---|---|---|---|
 * | PERMANENT_TEMP_ALL          | 1-1 | -        | 상시,임시 | O | - | -    | 여사원소속 | - |
 * | PERMANENT_ONLY_EXCL_CONSIGN | 1-2 | -        | 상시      | O | O | -    | 여사원소속 | - |
 * | TEMP_ALL                    | 1-4 | -        | 임시      | - | - | -    | 여사원소속 | - |
 * | TEMP_ONLY_EXCL_CONSIGN      | 1-5 | -        | 임시      | - | O | -    | 여사원소속 | - |
 * | TEAM2_PERMANENT_TEMP_ALL    | 2-1 | -        | 상시,임시 | O | - | 4889 | 거래처    | - |
 * | AGENCY_PERMANENT_TEMP_ALL   | 3-1 | 대리점       | (전체)    | O | - | -    | 거래처    | O |
 * | AGENCY_PERMANENT_ONLY       | 3-2 | 대리점       | 상시      | O | - | -    | 거래처    | O |
 * | AGENCY_TEMP_ONLY            | 3-3 | 대리점       | 임시      | - | - | -    | 거래처    | O |
 * | HYPERMARKET_PERMANENT       | -   | 대형마트(3대) | 상시      | O | - | -    | 여사원소속 | - |
 * | HYPERMARKET_PERMANENT_WC3   | -   | 대형마트(3대) | 상시      | O | - | -    | 여사원소속 | O |
 *
 * 비고(SF 정합): 대형마트 2종의 구분필터 `대형마트(3대)` 는 Account.Type picklist(할인점/체인/.../기타 14종)에
 *   존재하지 않는 값 — SF Report 에서도 0건만 반환하는 죽은 필터. 신규도 0건 반환으로 SF 동작 동일 (사용자 결정: 원문대로 이식).
 *
 * @param accountTypeFilter  구분(Account.accountType.displayName) equals 필터. null = 전체 유형.
 * @param includeWorkingCategory3  근무유형3 컬럼/그룹 표시 여부 (SF groupingsAcross WorkingCategory3__c 정합).
 */
enum class ConvertedHeadcountReportVariant(
    val reportName: String,
    val workingCategory5In: List<String>,
    val includeNullWc5: Boolean,
    val excludeConsignment: Boolean,
    val costCenterCode: String?,
    val useAccountBranch: Boolean,
    val accountTypeFilter: String? = null,
    val includeWorkingCategory3: Boolean = false,
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

    // -- 대리점 3종 (구분=대리점, 지점=거래처, 근무유형3 컬럼 포함) --
    AGENCY_PERMANENT_TEMP_ALL(
        reportName = "대리점환산인원(상시임시전체)",
        workingCategory5In = emptyList(), // 근무유형5 필터 없음 = 전체
        includeNullWc5 = false,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = true,
        accountTypeFilter = "대리점",
        includeWorkingCategory3 = true,
    ),
    AGENCY_PERMANENT_ONLY(
        reportName = "대리점환산인원(상시)",
        workingCategory5In = listOf("상시"),
        includeNullWc5 = true,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = true,
        accountTypeFilter = "대리점",
        includeWorkingCategory3 = true,
    ),
    AGENCY_TEMP_ONLY(
        reportName = "대리점환산인원(임시)",
        workingCategory5In = listOf("임시"),
        includeNullWc5 = false,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = true,
        accountTypeFilter = "대리점",
        includeWorkingCategory3 = true,
    ),

    // -- 대형마트 2종 (구분=대형마트(3대) — SF 0건 죽은 필터, 지점=여사원소속) --
    HYPERMARKET_PERMANENT(
        reportName = "대형마트환산인원(상시)",
        workingCategory5In = listOf("상시"),
        includeNullWc5 = true,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = false,
        accountTypeFilter = "대형마트(3대)",
        includeWorkingCategory3 = false,
    ),
    HYPERMARKET_PERMANENT_WC3(
        reportName = "대형마트환산인원(상시_근무유형3추가)",
        workingCategory5In = listOf("상시"),
        includeNullWc5 = true,
        excludeConsignment = false,
        costCenterCode = null,
        useAccountBranch = false,
        accountTypeFilter = "대형마트(3대)",
        includeWorkingCategory3 = true,
    ),
}
