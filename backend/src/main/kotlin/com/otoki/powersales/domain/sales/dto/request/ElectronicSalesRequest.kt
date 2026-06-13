package com.otoki.powersales.domain.sales.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/**
 * 전산매출(ABC) 조회 요청 DTO (query params).
 *
 * 레거시 `promotion/month/abcmain.jsp` 대응 — 거래처 1곳 + 기간(daterangepicker) + 매출 조회 제품
 * (바코드 목록) 으로 제품별 실적을 조회한다. `customerId` 는 거래처(`Account`) PK 이며 POS
 * `live_tot_sales_dh` 의 `CUST_CD` 로 변환된다.
 *
 * ## 레거시 정합
 * - 기간: `daterangepicker` 의 시작일~종료일 → [startDate]/[endDate] (`YMD_ID BETWEEN`).
 * - 매출 조회 제품: 제품명 팝업/바코드로 추가한 제품의 바코드 목록 → [barcodes] (`UPC_CD IN`).
 *   비어 있으면 레거시 `abcSumAmount` 처럼 합계금액만 조회한다.
 */
data class ElectronicSalesRequest(
	@field:NotNull(message = "거래처 ID는 필수입니다")
	val customerId: Long,

	@field:NotBlank(message = "조회 시작일은 필수입니다")
	@field:Pattern(
		regexp = "^\\d{4}-\\d{2}-\\d{2}$",
		message = "시작일 형식은 YYYY-MM-DD 이어야 합니다"
	)
	val startDate: String,

	@field:NotBlank(message = "조회 종료일은 필수입니다")
	@field:Pattern(
		regexp = "^\\d{4}-\\d{2}-\\d{2}$",
		message = "종료일 형식은 YYYY-MM-DD 이어야 합니다"
	)
	val endDate: String,

	/** 매출 조회 제품의 바코드 목록 (레거시 `UPC_CD IN`). 비어 있으면 합계금액만 조회. */
	val barcodes: List<String>? = null,
)
