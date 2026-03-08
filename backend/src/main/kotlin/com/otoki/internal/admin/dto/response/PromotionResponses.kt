package com.otoki.internal.admin.dto.response

import com.otoki.internal.promotion.entity.Promotion
import java.time.LocalDate
import java.time.LocalDateTime

data class PromotionListResponse(
    val content: List<PromotionListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class PromotionListItem(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String,
    val promotionType: String?,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val targetAmount: Long?,
    val category: String?,
    val costCenterCode: String?,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            category: String?
        ): PromotionListItem = PromotionListItem(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionType = promotion.promotionType,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            targetAmount = promotion.targetAmount,
            category = category,
            costCenterCode = promotion.costCenterCode,
            isDeleted = promotion.isDeleted,
            createdAt = promotion.createdAt
        )
    }
}

data class PromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String,
    val promotionType: String?,
    val accountId: Int,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val primaryProductId: Long?,
    val primaryProductName: String?,
    val otherProduct: String?,
    val message: String?,
    val standLocation: String?,
    val targetAmount: Long?,
    val costCenterCode: String?,
    val category: String?,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            category: String?
        ): PromotionDetailResponse = PromotionDetailResponse(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionType = promotion.promotionType,
            accountId = promotion.accountId,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            primaryProductId = promotion.primaryProductId,
            primaryProductName = primaryProductName,
            otherProduct = promotion.otherProduct,
            message = promotion.message,
            standLocation = promotion.standLocation,
            targetAmount = promotion.targetAmount,
            costCenterCode = promotion.costCenterCode,
            category = category,
            isDeleted = promotion.isDeleted,
            createdAt = promotion.createdAt,
            updatedAt = promotion.updatedAt
        )
    }
}
