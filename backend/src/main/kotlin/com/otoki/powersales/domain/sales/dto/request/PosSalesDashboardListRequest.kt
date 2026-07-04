package com.otoki.powersales.domain.sales.dto.request

import java.time.LocalDate

/**
 * POS매출 대시보드 2단(POS 집계) list endpoint 요청 파라미터.
 *
 * 「POS매출」 web admin 화면 — 레거시 `promotion/month/posmain.jsp` (POS `live_pos_sales_dh`)
 * 의 거래처별 확장. 성능 보호를 위해 2단 조회로 분리 — 1단(`/accounts`)에서 조건에 맞는 거래처
 * 목록을 메인 DB 로 조회한 뒤, 운영자가 선택한 거래처(accountIds, 최대 상한)만 이 요청으로
 * 외부 POS DB 집계한다. startDate/endDate(일 단위, 레거시 daterangepicker 정합 — 최대 31일) +
 * accountIds(선택 거래처, 필수) 로 조회한다.
 *
 * 필터 해소 위치:
 * - 메인 DB(Account): accountIds 로 직접 선택 (1단에서 유통형태/거래처유형/거래처명 필터 적용 완료)
 * - 메인 DB(Product) → POS `BARCODE IN` 합류: productIds(선택 제품) / category2(중분류) / category3(소분류)
 *
 * page / size / sort 는 페이징.
 */
data class PosSalesDashboardListRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** 2단 조회 대상 거래처 id (1단에서 선택). 최대 상한 초과 시 400. */
    val accountIds: List<Long>,
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
