package com.otoki.powersales.domain.activity.productexpiration.dto.response

import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ProductExpirationItemResponse(
    val seq: Int,
    val productCode: String,
    val productName: String,
    val accountCode: String,
    val accountName: String,
    val expirationDate: String,
    val alarmDate: String,
    val dDay: Int,
    val description: String?,
    val isExpired: Boolean
) {
    companion object {
        fun from(entity: ProductExpiration): ProductExpirationItemResponse = from(entity, LocalDate.now())

        fun from(entity: ProductExpiration, today: LocalDate): ProductExpirationItemResponse {
            val dDay = ChronoUnit.DAYS.between(today, entity.expirationDate).toInt()
            return ProductExpirationItemResponse(
                seq = entity.seq,
                productCode = entity.productCode ?: "",
                productName = entity.productName ?: "",
                accountCode = entity.accountCode ?: "",
                accountName = entity.accountName ?: "",
                expirationDate = entity.expirationDate.toString(),
                alarmDate = entity.alarmDate.toString(),
                dDay = dDay,
                description = entity.description,
                isExpired = dDay <= 0
            )
        }
    }
}

data class ProductExpirationBatchDeleteResponse(
    val deletedCount: Int
)
