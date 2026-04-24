package com.otoki.powersales.admin.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class AdminProductExpirationResponse(
    val id: Int,
    val seq: Int,
    val productName: String,
    val productCode: String,
    val accountName: String,
    val accountCode: String,
    val employeeName: String,
    val employeeCode: String,
    val expirationDate: String,
    val alarmDate: String,
    @get:JsonProperty("d_day")
    val dDay: Int,
    val status: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: ProductExpiration): AdminProductExpirationResponse =
            from(entity, LocalDate.now())

        fun from(entity: ProductExpiration, today: LocalDate): AdminProductExpirationResponse {
            val expDate = entity.expirationDate
            val dDay = if (expDate != null) ChronoUnit.DAYS.between(today, expDate).toInt() else 0
            val status = when {
                dDay <= 0 -> "EXPIRED"
                dDay <= 7 -> "IMMINENT"
                else -> "NORMAL"
            }

            return AdminProductExpirationResponse(
                id = entity.productExpirationId,
                seq = entity.seq,
                productName = entity.productName ?: "",
                productCode = entity.productCode ?: "",
                accountName = entity.accountName ?: "",
                accountCode = entity.accountCode ?: "",
                employeeName = entity.employee?.name ?: "",
                employeeCode = entity.employee?.employeeCode ?: "",
                expirationDate = expDate?.toString() ?: "",
                alarmDate = entity.alarmDate?.toString() ?: "",
                dDay = dDay,
                status = status,
                description = entity.description,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

data class AdminProductExpirationListResponse(
    val content: List<AdminProductExpirationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AdminProductExpirationSummaryResponse(
    val totalCount: Long,
    val expiredCount: Long,
    val imminentCount: Long,
    val normalCount: Long
)

data class AdminProductExpirationBatchDeleteResponse(
    val deletedCount: Int
)
