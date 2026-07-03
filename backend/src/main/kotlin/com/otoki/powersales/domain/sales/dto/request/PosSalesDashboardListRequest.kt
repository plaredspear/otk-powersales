package com.otoki.powersales.domain.sales.dto.request

import java.time.LocalDate

/**
 * POS매출 대시보드 list endpoint 요청 파라미터.

 * 「POS매출」 web admin 화면 — 레거시 `promotion/month/posmain.jsp` (POS `live_pos_sales_dh`)
 * 의 거래처별 확장. startDate/endDate(일 단위, 레거시 daterangepicker 정합 — 최대 31일) +
 * costCenterCodes 는 필수.
 *
 * 필터 해소 위치 (전산실적 [ElectronicSalesDashboardListRequest] 와 동일 정책):
 * - 메인 DB(Account): customerKeyword / distributionChannels(유통형태 라벨) /
 *   accountTypes(거래처유형 = ABC유형 라벨)
 * - 메인 DB(Product) → POS `BARCODE IN` 합류: productIds(선택 제품) / category2(중분류) / category3(소분류)
 *
 * page / size / sort 는 페이징.
 */
data class PosSalesDashboardListRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val costCenterCodes: List<String>,
    val customerKeyword: String? = null,
    /** 유통형태 라벨 (거래처상태코드+거래처타입 조합, 예 "02 슈퍼"). 비우면 전체. */
    val distributionChannels: List<String> = emptyList(),
    /** 거래처유형 라벨 (ABC유형코드+ABC유형 조합, 예 "6111 이마트"). 비우면 전체. */
    val accountTypes: List<String> = emptyList(),
    /** 조회 제품 (다중 선택). 비우면 전체 — category2/3 와 AND 결합. */
    val productIds: List<Long> = emptyList(),
    /** 제품 중분류 (Product.category2). */
    val category2: String? = null,
    /** 제품 소분류 (Product.category3). */
    val category3: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
) {
    /** 제품/분류 필터가 하나라도 지정되었는지 — POS 쿼리의 `BARCODE IN` 분기 여부. */
    fun hasProductFilter(): Boolean =
        productIds.isNotEmpty() || !category2.isNullOrBlank() || !category3.isNullOrBlank()
}
