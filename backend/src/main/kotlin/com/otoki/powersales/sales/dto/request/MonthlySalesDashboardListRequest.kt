package com.otoki.powersales.sales.dto.request

/**
 * 월매출 대시보드 list endpoint 요청 파라미터.
 *
 * year/month + costCenterCodes 는 필수 (인접 admin endpoint 컨벤션). accountIds / customerKeyword / accountGroup 은 추가 필터. page / size / sort 는 페이징.
 */
data class MonthlySalesDashboardListRequest(
    val year: Int,
    val month: Int,
    val costCenterCodes: List<String>,
    val accountIds: List<Int> = emptyList(),
    val accountGroup: String? = null,
    val customerKeyword: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
)
