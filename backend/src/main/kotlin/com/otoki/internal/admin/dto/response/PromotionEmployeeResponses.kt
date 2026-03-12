package com.otoki.internal.admin.dto.response

import com.otoki.internal.promotion.entity.PromotionEmployee
import java.time.LocalDate
import java.time.LocalDateTime

data class PromotionEmployeeListResponse(
    val id: Long,
    val promotionId: Long,
    val employeeSfid: String?,
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
    val actualAmount: Long?
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): PromotionEmployeeListResponse =
            PromotionEmployeeListResponse(
                id = entity.id,
                promotionId = entity.promotionId,
                employeeSfid = entity.employeeSfid,
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
                actualAmount = entity.actualAmount
            )
    }
}

data class PromotionEmployeeDetailResponse(
    val id: Long,
    val promotionId: Long,
    val employeeSfid: String?,
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): PromotionEmployeeDetailResponse =
            PromotionEmployeeDetailResponse(
                id = entity.id,
                promotionId = entity.promotionId,
                employeeSfid = entity.employeeSfid,
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
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
    }
}
