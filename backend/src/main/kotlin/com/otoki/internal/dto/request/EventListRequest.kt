/*
package com.otoki.internal.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

/ **
 * 행사 목록 조회 요청 DTO
 * /
data class EventListRequest(
    val customerId: String? = null,

    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "날짜 형식은 YYYY-MM-DD 이어야 합니다"
    )
    val date: String? = null,

    @field:Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    val page: Int = 0,

    @field:Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @field:Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    val size: Int = 10
) {
    / **
     * 조회 기준일 (기본값: 오늘)
     * /
    fun getDateOrToday(): LocalDate {
        return if (date != null) {
            LocalDate.parse(date)
        } else {
            LocalDate.now()
        }
    }
}
*/
