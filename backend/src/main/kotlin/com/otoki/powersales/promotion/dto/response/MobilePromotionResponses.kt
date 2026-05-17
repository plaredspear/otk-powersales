package com.otoki.powersales.promotion.dto.response

import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
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
    val promotionType: String?,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val standLocation: String?,
    val isClosed: Boolean,
    val myScheduleDate: LocalDate?
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            myScheduleDate: LocalDate?
        ): MobilePromotionListItem = MobilePromotionListItem(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionType = promotion.promotionType?.displayName,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            standLocation = promotion.standLocation?.displayName,
            isClosed = promotion.isClosed,
            myScheduleDate = myScheduleDate
        )
    }
}

data class MobilePromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionType: String?,
    val accountId: Int,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val primaryProductName: String?,
    val otherProduct: String?,
    val message: String?,
    val standLocation: String?,
    val productType: String?,
    val isClosed: Boolean,
    val remark: String?,
    val employees: List<MobilePromotionEmployeeItem>
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            employees: List<MobilePromotionEmployeeItem>
        ): MobilePromotionDetailResponse = MobilePromotionDetailResponse(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionType = promotion.promotionType?.displayName,
            accountId = promotion.account!!.id,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            primaryProductName = primaryProductName,
            otherProduct = promotion.otherProduct,
            message = promotion.message,
            standLocation = promotion.standLocation?.displayName,
            productType = promotion.productType?.displayName,
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
    val targetAmount: Long?,
    val actualAmount: Long?
) {
    companion object {
        fun from(entity: PromotionEmployee, employeeName: String?): MobilePromotionEmployeeItem =
            MobilePromotionEmployeeItem(
                id = entity.id,
                employeeName = employeeName,
                scheduleDate = entity.scheduleDate,
                workStatus = entity.workStatus?.displayName,
                workType3 = entity.workType3?.displayName,
                targetAmount = entity.targetAmount,
                actualAmount = entity.actualAmount
            )
    }
}
