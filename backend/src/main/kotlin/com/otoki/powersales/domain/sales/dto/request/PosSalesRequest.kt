package com.otoki.powersales.domain.sales.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/**
 * POS매출 조회 요청 DTO (query params).
 *
 * 레거시 `promotion/month/posmain.jsp` 대응 — 거래처 1곳 + 연월 단위, 제품별 POS 스캔 실적 조회.
 * `customerId` 는 거래처(`Account`) PK. POS `live_pos_sales_dh` 의 `CUST_CD` 로 변환.
 */
data class PosSalesRequest(
	@field:NotNull(message = "거래처 ID는 필수입니다")
	val customerId: Long,

	@field:NotBlank(message = "연월은 필수입니다")
	@field:Pattern(
		regexp = "^\\d{6}$",
		message = "연월 형식은 YYYYMM 이어야 합니다"
	)
	val yearMonth: String,
)
