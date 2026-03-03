package com.otoki.internal.shelflife.dto.response

import com.otoki.internal.shelflife.entity.ShelfLife
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ShelfLifeItemResponse(
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
        fun from(entity: ShelfLife): ShelfLifeItemResponse = from(entity, LocalDate.now())

        fun from(entity: ShelfLife, today: LocalDate): ShelfLifeItemResponse {
            val dDay = ChronoUnit.DAYS.between(today, entity.expirationDate).toInt()
            return ShelfLifeItemResponse(
                seq = entity.seq,
                productCode = entity.productCode ?: "",
                productName = entity.productId ?: "",
                accountCode = entity.accountCode ?: "",
                accountName = entity.accountId ?: "",
                expirationDate = entity.expirationDate.toString(),
                alarmDate = entity.alarmDate.toString(),
                dDay = dDay,
                description = entity.description,
                isExpired = dDay <= 0
            )
        }
    }
}

data class ShelfLifeBatchDeleteResponse(
    val deletedCount: Int
)
