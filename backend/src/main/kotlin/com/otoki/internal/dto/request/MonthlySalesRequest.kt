package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * 월매출 조회 요청 DTO
 */
data class MonthlySalesRequest(
    val customerId: String? = null,

    @field:NotBlank(message = "연월은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{6}$",
        message = "연월 형식은 YYYYMM 이어야 합니다"
    )
    val yearMonth: String
) {
    /**
     * 연도 추출
     */
    fun getYear(): Int = yearMonth.substring(0, 4).toInt()

    /**
     * 월 추출
     */
    fun getMonth(): Int = yearMonth.substring(4, 6).toInt()

    /**
     * 전년 동월 (YYYYMM)
     */
    fun getPreviousYearMonth(): String {
        val year = getYear() - 1
        val month = getMonth()
        return String.format("%04d%02d", year, month)
    }
}
