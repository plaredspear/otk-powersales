package com.otoki.powersales.sales.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

/**
 * 물류매출 조회 요청 DTO (query params).
 *
 * 레거시 `promotion/month/list.jsp` (월 매출 조회/물류) 대응 — 거래처 1곳 + 연월 단위 조회.
 * `customerId` 는 거래처(`Account`) PK. 바인딩 누락 시 [NotNull] 으로 400 처리.
 */
data class LogisticsSalesRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    val customerId: Int,

    @field:NotBlank(message = "연월은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{6}$",
        message = "연월 형식은 YYYYMM 이어야 합니다"
    )
    val yearMonth: String
)
