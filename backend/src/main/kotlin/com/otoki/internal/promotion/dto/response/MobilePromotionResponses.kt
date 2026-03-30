package com.otoki.internal.promotion.dto.response

import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import java.time.LocalDate

data class MobilePromotionListResponse(
    val content: List<MobilePromotionListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class MobilePromotionListItem(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String?,
    val promotionTypeName: String?,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val category: String?,
    val standLocation: String?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val isClosed: Boolean,
    val myScheduleDate: LocalDate?
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            promotionTypeName: String?,
            myScheduleDate: LocalDate?
        ): MobilePromotionListItem = MobilePromotionListItem(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionTypeName = promotionTypeName,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            category = promotion.category,
            standLocation = promotion.standLocation,
            targetAmount = promotion.targetAmount,
            actualAmount = promotion.actualAmount,
            isClosed = promotion.isClosed,
            myScheduleDate = myScheduleDate
        )
    }
}

data class MobilePromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String?,
    val promotionTypeName: String?,
    val accountId: Int,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val primaryProductName: String?,
    val otherProduct: String?,
    val message: String?,
    val standLocation: String?,
    val category: String?,
    val productType: String?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val isClosed: Boolean,
    val remark: String?,
    val employees: List<MobilePromotionEmployeeItem>
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            promotionTypeName: String?,
            employees: List<MobilePromotionEmployeeItem>
        ): MobilePromotionDetailResponse = MobilePromotionDetailResponse(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionTypeName = promotionTypeName,
            accountId = promotion.account.id,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            primaryProductName = primaryProductName,
            otherProduct = promotion.otherProduct,
            message = promotion.message,
            standLocation = promotion.standLocation,
            category = promotion.category,
            productType = promotion.productType,
            targetAmount = promotion.targetAmount,
            actualAmount = promotion.actualAmount,
            isClosed = promotion.isClosed,
            remark = promotion.remark,
            employees = employees
        )
    }
}

data class MobilePromotionEmployeeItem(
    val id: Long,
    val employeeName: String?,
    val scheduleDate: LocalDate?,
    val workStatus: String?,
    val workType3: String?,
    val professionalPromotionTeam: String?,
    val targetAmount: Long?,
    val actualAmount: Long?
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): MobilePromotionEmployeeItem =
            MobilePromotionEmployeeItem(
                id = entity.id,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus,
                workType3 = entity.workType3,
                professionalPromotionTeam = entity.professionalPromotionTeam,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount
            )
    }
}
