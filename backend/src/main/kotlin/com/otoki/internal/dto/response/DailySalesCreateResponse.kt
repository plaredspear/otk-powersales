package com.otoki.internal.dto.response

import com.otoki.internal.entity.DailySales
import java.time.format.DateTimeFormatter

/**
 * 일매출 등록/임시저장 응답 DTO
 */
data class DailySalesCreateResponse(
    val dailySalesId: String,
    val salesDate: String,
    val totalAmount: Int?,
    val status: String,
    val registeredAt: String
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        /**
         * DailySales Entity로부터 응답 DTO 생성
         */
        fun from(dailySales: DailySales): DailySalesCreateResponse {
            val totalAmount = calculateTotalAmount(dailySales)

            return DailySalesCreateResponse(
                dailySalesId = dailySales.id.toString(),
                salesDate = dailySales.salesDate.format(DATE_FORMATTER),
                totalAmount = totalAmount,
                status = dailySales.status,
                registeredAt = dailySales.createdAt.format(DATETIME_FORMATTER)
            )
        }

        /**
         * 총 판매금액 계산 (대표제품 + 기타제품)
         */
        private fun calculateTotalAmount(dailySales: DailySales): Int? {
            val mainAmount = dailySales.mainProductAmount ?: 0
            val subAmount = dailySales.subProductAmount ?: 0
            val total = mainAmount + subAmount

            return if (total > 0) total else null
        }
    }
}
