package com.otoki.powersales.sales.dto.response

/**
 * 전산매출(ABC) 조회 응답 DTO.
 *
 * ## 데이터 source
 * POS DB `public.live_tot_sales_dh` ([com.otoki.pos.entity.LiveTotSalesDaily]) — 레거시 Heroku
 * `PosMapper.xml#SelectAbcData` 가 조회하던 제품·일별 전산매출. 거래처 1곳 + 기간의 제품별
 * `SUM(SALES_RAMT)`(금액) / `SUM(SALES_RQTY)`(수량) 집계.
 *
 * ## 레거시 deviation
 * - 레거시 ABC 화면은 제품별 명세에 전년 동월 비교가 없어 `previousYearAmount`/`growthRate` 미산출
 *   (물류매출과 달리 ORORA 월마감 전년치 소스를 쓰지 않음). 모바일 `ElectronicSales` 의 해당 필드는
 *   nullable 이라 생략한다.
 */
data class ElectronicSalesResponse(
	val customerId: Long,
	val customerName: String,
	val sapAccountCode: String,
	val yearMonth: String,
	val items: List<ProductSales>,
) {
	/** 제품별 전산매출 실적. */
	data class ProductSales(
		val productCode: String,
		val productName: String,
		/** 매출 실적 금액(원). `SUM(SALES_RAMT)`. */
		val amount: Long,
		/** 매출 실적 수량. `SUM(SALES_RQTY)`. */
		val quantity: Long,
	)
}
