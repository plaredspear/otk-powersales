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
    val promotionName: String?,
    val promotionTypeId: Long?,
    val promotionTypeName: String?,
    val accountName: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val category: String?,
    val productType: String?,
    val branchName: String?,
    val isClosed: Boolean,
    val costCenterCode: String?,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val remark: String? = null
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            promotionTypeName: String?
        ): PromotionListItem = PromotionListItem(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionTypeId = promotion.promotionTypeId,
            promotionTypeName = promotionTypeName,
            accountName = accountName,
            startDate = promotion.startDate,
            endDate = promotion.endDate,
            targetAmount = promotion.targetAmount,
            actualAmount = promotion.actualAmount,
            category = promotion.category,
            productType = promotion.productType,
            branchName = promotion.branchName,
            isClosed = promotion.isClosed,
            costCenterCode = promotion.costCenterCode,
            isDeleted = promotion.isDeleted,
            createdAt = promotion.createdAt,
            remark = promotion.remark
        )
    }
}

data class PromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String?,
    val promotionTypeId: Long?,
    val promotionTypeName: String?,
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
    val actualAmount: Long?,
    val costCenterCode: String?,
    val category: String?,
    val productType: String?,
    val branchName: String?,
    val isClosed: Boolean,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remark: String? = null
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            primaryProductName: String?,
            promotionTypeName: String?
        ): PromotionDetailResponse = PromotionDetailResponse(
            id = promotion.id,
            promotionNumber = promotion.promotionNumber,
            promotionName = promotion.promotionName,
            promotionTypeId = promotion.promotionTypeId,
            promotionTypeName = promotionTypeName,
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
            actualAmount = promotion.actualAmount,
            costCenterCode = promotion.costCenterCode,
            category = promotion.category,
            productType = promotion.productType,
            branchName = promotion.branchName,
            isClosed = promotion.isClosed,
            isDeleted = promotion.isDeleted,
            createdAt = promotion.createdAt,
            updatedAt = promotion.updatedAt,
            remark = promotion.remark
        )
    }
}
