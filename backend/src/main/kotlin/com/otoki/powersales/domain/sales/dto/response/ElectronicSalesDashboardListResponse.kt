package com.otoki.powersales.domain.sales.dto.response

import java.time.LocalDate

/**
 * 전산실적 대시보드 거래처별 명세 응답.
 *
 * 「월 매출(전산실적)」 web admin 화면의 하단 테이블 — 권한 범위 거래처 N건의 기간(startDate~endDate)
 * 전산매출(POS `live_tot_sales_dh`) 합계를 거래처 1행으로 표현. row 클릭 시 제품별 상세
 * ([ElectronicSalesDashboardDetailResponse]) 조회.
 *
 * @property totalSalesAmount 조회 결과 전체(페이징 무관)의 전산매출 금액 합계 — 화면 상단 합계 표시용
 * @property totalSalesQuantity 조회 결과 전체(페이징 무관)의 전산매출 수량 합계
 */
data class ElectronicSalesDashboardListResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalSalesAmount: Long,
    val totalSalesQuantity: Long,
    val items: List<ElectronicSalesDashboardListItem>,
    val pageInfo: PageInfo,
) {
    data class PageInfo(
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
}

/**
 * 전산실적 명세 테이블 한 행 — 거래처 단위 기간 전산매출 합계.
 *
 * @property salesAmount 전산매출 금액 합계 (원) — POS `SUM(SALES_RAMT)`
 * @property salesQuantity 전산매출 수량 합계 — POS `SUM(SALES_RQTY)`
 */
data class ElectronicSalesDashboardListItem(
    val accountId: Long,
    val accountName: String?,
    val sapAccountCode: String?,
    val branchCode: String?,
    val branchName: String?,
    val salesAmount: Long,
    val salesQuantity: Long,
)
