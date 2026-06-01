package com.otoki.powersales.sales.dto.response

/**
 * POS매출 조회 응답 DTO.
 *
 * ## 데이터 source
 * POS DB `public.live_pos_sales_dh` ([com.otoki.pos.entity.LivePosSalesDaily]) — 레거시 Heroku
 * `PosMapper.xml#selectPosData` 가 조회하던 제품·일별 POS매출. 거래처 1곳 + 기간의 제품별
 * `SUM(SALES_AMT)`(금액) / `SUM(SALES_QTY)`(수량) 집계.
 *
 * ## 레거시 deviation
 * - 레거시 posmain.jsp 는 ① 품목 미지정 시 합계만(`posSumAmount`, 채널 매핑 JOIN) ② 품목 지정 시
 *   품목별 명세(`selectPosData`) 두 모드였다. 신규 web admin 은 거래처+연월 단위로 **항상 제품별
 *   명세 + 클라이언트 합산** 으로 통일 (전산매출 화면과 동일 UX). 채널 매핑 합계 SQL 은 미이관.
 * - 레거시 BARCODE group key 는 제품 단위 집계로 단순화하고 대표 바코드 1건만 노출.
 */
data class PosSalesResponse(
	val customerId: Int,
	val customerName: String,
	val sapAccountCode: String,
	val yearMonth: String,
	val items: List<ProductSales>,
) {
	/** 제품별 POS매출 실적. */
	data class ProductSales(
		val productCode: String,
		val productName: String,
		/** 대표 바코드 (`MAX(BARCODE)`). */
		val barcode: String?,
		/** 매출 금액(원). `SUM(SALES_AMT)`. */
		val amount: Long,
		/** 매출 수량(EA). `SUM(SALES_QTY)`. */
		val quantity: Long,
	)
}
