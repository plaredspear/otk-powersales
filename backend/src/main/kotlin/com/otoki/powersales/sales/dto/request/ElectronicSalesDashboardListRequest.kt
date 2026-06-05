package com.otoki.powersales.sales.dto.request

/**
 * 전산실적 대시보드 list endpoint 요청 파라미터.
 *
 * 「월 매출(전산실적)」 web admin 화면 — 레거시 `promotion/month/abcmain.jsp` (POS `live_tot_sales_dh`)
 * 동등. year/month + costCenterCodes 는 필수. accountIds / customerKeyword / accountGroup 은 추가 필터.
 * page / size / sort 는 페이징.
 */
data class ElectronicSalesDashboardListRequest(
    val year: Int,
    val month: Int,
    val costCenterCodes: List<String>,
    val accountIds: List<Long> = emptyList(),
    val accountGroup: String? = null,
    val customerKeyword: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
)
