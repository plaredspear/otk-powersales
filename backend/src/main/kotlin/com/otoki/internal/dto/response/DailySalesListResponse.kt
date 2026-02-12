package com.otoki.internal.dto.response

import java.time.LocalDateTime

/**
 * 일별 매출 목록 응답 DTO
 */
data class DailySalesListResponse(
    val dailySales: List<DailySalesInfo>
) {

    /**
     * 일별 매출 정보
     */
    data class DailySalesInfo(
        val dailySalesId: String,
        val salesDate: String,
        val totalAmount: Long,
        val status: String,
        val registeredAt: LocalDateTime
    )
}
