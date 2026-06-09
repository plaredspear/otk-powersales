package com.otoki.powersales.sales.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/**
 * POS매출 조회 요청 DTO (date range, query params).
 *
 * 레거시 `promotion/month/posmain.jsp` 의 daterangepicker 기반 조회 정합 — 거래처 1곳 + 기간(시작/종료일)
 * 단위. 매출 조회 제품(`#add-prd-list`) 에서 선택한 바코드 목록([barcodes]) 으로 품목별 명세를 필터한다.
 *
 * - [barcodes] 미지정/빈 목록: 거래처 전체 제품 집계 (레거시 `posSumAmount` 합계 모드 대응).
 * - [barcodes] 1건 이상: 해당 바코드 제품만 집계 (레거시 `posAmount` 명세 모드 대응).
 *
 * `customerId` 는 거래처(`Account`) PK. POS `live_pos_sales_dh` 의 `CUST_CD` 로 변환.
 */
data class PosSalesRangeRequest(
	@field:NotNull(message = "거래처 ID는 필수입니다")
	val customerId: Long,

	@field:NotBlank(message = "시작일은 필수입니다")
	@field:Pattern(
		regexp = "^\\d{4}-\\d{2}-\\d{2}$",
		message = "시작일 형식은 YYYY-MM-DD 이어야 합니다"
	)
	val startDate: String,

	@field:NotBlank(message = "종료일은 필수입니다")
	@field:Pattern(
		regexp = "^\\d{4}-\\d{2}-\\d{2}$",
		message = "종료일 형식은 YYYY-MM-DD 이어야 합니다"
	)
	val endDate: String,

	/**
	 * 매출 조회 제품의 바코드 목록 (선택, 쉼표 구분 문자열).
	 *
	 * 예: `"8801043011234,8801043015678"`. 비어 있으면 거래처 전체 제품 집계.
	 * 쉼표 구분 문자열로 받아 [barcodeList] 로 파싱한다 (생성자 바인딩의 List 모호성 회피).
	 */
	val barcodes: String? = null,
) {
	/** [barcodes] 를 공백 제거·빈값 제외한 바코드 목록으로 파싱. */
	fun barcodeList(): List<String> =
		barcodes?.split(",")?.mapNotNull { it.trim().ifBlank { null } } ?: emptyList()
}
