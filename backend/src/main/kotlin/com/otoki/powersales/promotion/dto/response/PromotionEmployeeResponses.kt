package com.otoki.powersales.promotion.dto.response

import com.otoki.powersales.promotion.entity.PromotionEmployee
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal

data class PromotionEmployeeListResponse(
    val id: Long,
    val name: String?,
    val promotionId: Long,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: BigDecimal?,
    val dailyTargetCount: BigDecimal?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primarySalesPrice: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val s3ImageUniqueKey: String?
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?, employeeCode: String? = null): PromotionEmployeeListResponse =
            PromotionEmployeeListResponse(
                id = entity.id,
                name = entity.name,
                promotionId = entity.promotionId!!,
                employeeId = entity.employeeId,
                employeeCode = employeeCode,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType1 = entity.workType1?.displayName,
                workType3 = entity.workType3?.displayName,
                scheduleId = entity.teamMemberScheduleId,
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
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType1: String?,
    val workType3: String?,
    val scheduleId: Long?,
    val promoCloseByTm: Boolean,
    val basePrice: BigDecimal?,
    val dailyTargetCount: BigDecimal?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val primaryProductAmount: BigDecimal?,
    val primarySalesQuantity: BigDecimal?,
    val primarySalesPrice: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val s3ImageUniqueKey: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?, employeeCode: String? = null): PromotionEmployeeDetailResponse =
            PromotionEmployeeDetailResponse(
                id = entity.id,
                promotionId = entity.promotionId!!,
                employeeId = entity.employeeId,
                employeeCode = employeeCode,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType1 = entity.workType1?.displayName,
                workType3 = entity.workType3?.displayName,
                scheduleId = entity.teamMemberScheduleId,
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
