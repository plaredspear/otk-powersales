package com.otoki.internal.admin.dto.response

import com.otoki.internal.promotion.entity.PromotionEmployee
import java.time.LocalDate
import java.time.LocalDateTime

data class PromotionEmployeeListResponse(
    val id: Long,
    val promotionId: Long,
    val employeeId: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    val workType4: String?,
    val professionalPromotionTeam: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: Long?,
    val dailyTargetCount: Int?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: Long?,
    val primarySalesQuantity: Int?,
    val primarySalesPrice: Long?,
    val otherSalesAmount: Long?,
    val otherSalesQuantity: Int?,
    val s3ImageUniqueKey: String?
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): PromotionEmployeeListResponse =
            PromotionEmployeeListResponse(
                id = entity.id,
                promotionId = entity.promotionId,
                employeeId = entity.employeeId,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus,
                workType1 = entity.workType1,
                workType3 = entity.workType3,
                workType4 = entity.workType4,
                professionalPromotionTeam = entity.professionalPromotionTeam,
                scheduleId = entity.scheduleId,
                promoCloseByTm = entity.promoCloseByTm,
                basePrice = entity.basePrice,
                dailyTargetCount = entity.dailyTargetCount,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount,
                primaryProductAmount = entity.primaryProductAmount,
                primarySalesQuantity = entity.primarySalesQuantity,
                primarySalesPrice = entity.primarySalesPrice,
                otherSalesAmount = entity.otherSalesAmount,
                otherSalesQuantity = entity.otherSalesQuantity,
                s3ImageUniqueKey = entity.s3ImageUniqueKey
            )
    }
}

data class PromotionEmployeeDetailResponse(
    val id: Long,
    val promotionId: Long,
    val employeeId: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    val workType4: String?,
    val professionalPromotionTeam: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: Long?,
    val dailyTargetCount: Int?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: Long?,
    val primarySalesQuantity: Int?,
    val primarySalesPrice: Long?,
    val otherSalesAmount: Long?,
    val otherSalesQuantity: Int?,
    val s3ImageUniqueKey: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): PromotionEmployeeDetailResponse =
            PromotionEmployeeDetailResponse(
                id = entity.id,
                promotionId = entity.promotionId,
                employeeId = entity.employeeId,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus,
                workType1 = entity.workType1,
                workType3 = entity.workType3,
                workType4 = entity.workType4,
                professionalPromotionTeam = entity.professionalPromotionTeam,
                scheduleId = entity.scheduleId,
                promoCloseByTm = entity.promoCloseByTm,
                basePrice = entity.basePrice,
                dailyTargetCount = entity.dailyTargetCount,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount,
                primaryProductAmount = entity.primaryProductAmount,
                primarySalesQuantity = entity.primarySalesQuantity,
                primarySalesPrice = entity.primarySalesPrice,
                otherSalesAmount = entity.otherSalesAmount,
                otherSalesQuantity = entity.otherSalesQuantity,
                s3ImageUniqueKey = entity.s3ImageUniqueKey,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
    }
}
