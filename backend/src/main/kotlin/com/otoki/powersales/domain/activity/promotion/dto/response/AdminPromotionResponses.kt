package com.otoki.powersales.domain.activity.promotion.dto.response

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
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
    val promotionType: String?,
    val accountName: String?,
    val accountCode: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val primaryProductName: String?,
    val primaryProductCode: String?,
    val standLocation: String?,
    val productType: String?,
    val category1: String?,
    val isClosed: Boolean,
    val costCenterCode: String?,
    val targetAmount: Double?,
    val actualAmount: Double?,
    val createdById: Long?,
    val createdByName: String?,
    val isDeleted: Boolean,
    val createdAt: LocalDateTime,
    val remark: String? = null
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            accountCode: String?,
            primaryProductName: String?,
            primaryProductCode: String?,
            // 조원 파생값 SUM (SF rollup DKRetail__TargetAmount__c / ActualAmount__c 재현).
            targetAmount: Long,
            actualAmount: Long
        ): PromotionListItem {
            val productTypeName = promotion.productType
            // SF formula DKRetail__PromotionName__c = TEXT(DKRetail__ProductType__c) + '(' + DKRetail__PrimaryProductId__r.Name + ')'
            val promotionName = if (primaryProductName != null) {
                "${productTypeName.orEmpty()}(${primaryProductName})"
            } else null
            return PromotionListItem(
                id = promotion.id,
                promotionNumber = promotion.promotionNumber,
                promotionName = promotionName,
                promotionType = promotion.promotionType?.displayName,
                accountName = accountName,
                accountCode = accountCode,
                startDate = promotion.startDate,
                endDate = promotion.endDate,
                primaryProductName = primaryProductName,
                primaryProductCode = primaryProductCode,
                standLocation = promotion.standLocation?.displayName,
                productType = productTypeName,
                category1 = promotion.category1,
                isClosed = promotion.isClosed,
                costCenterCode = promotion.costCenterCode,
                // SF rollup DKRetail__TargetAmount__c / ActualAmount__c = 조원 파생값 SUM 재현.
                // (동기화 스칼라 dkTargetAmount/dkActualAmount 는 갱신 로직 없어 stale 이므로 미사용.)
                targetAmount = targetAmount.toDouble(),
                actualAmount = actualAmount.toDouble(),
                // 작성자 = SF CreatedBy 동등 (createdBy 는 searchForAdmin 에서 fetchJoin — N+1 없음).
                // id 는 목록의 작성자 → 사용자 상세(/users/:id) 링크용 (web 에서 user READ 권한 보유 시에만 링크).
                createdById = promotion.createdBy?.id,
                createdByName = promotion.createdBy?.name,
                isDeleted = promotion.isDeleted,
                createdAt = promotion.createdAt,
                remark = promotion.remark
            )
        }
    }
}

data class PromotionDetailResponse(
    val id: Long,
    val promotionNumber: String,
    val promotionName: String?,
    val promotionType: String?,
    val accountId: Long,
    val accountName: String?,
    val accountCode: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val primaryProductId: Long?,
    val primaryProductName: String?,
    val primaryProductCode: String?,
    val otherProduct: String?,
    val message: String?,
    val standLocation: String?,
    val costCenterCode: String?,
    val productType: String?,
    val category1: String?,
    val targetAmount: Long?,
    val actualAmount: Long?,
    val isClosed: Boolean,
    val isDeleted: Boolean,
    // 작성자 = SF CreatedBy 동등. createdBy 는 findByIdWithRelations 에서 fetchJoin (LazyInit 회피).
    // id 는 작성자 → 사용자 상세(/users/:id) 링크용 (web 에서 user READ 권한 보유 시에만 링크).
    val createdById: Long?,
    val createdByName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remark: String? = null
) {
    companion object {
        fun from(
            promotion: Promotion,
            accountName: String?,
            accountCode: String?,
            primaryProductName: String?,
            primaryProductCode: String?,
            // 조원 파생값 SUM (SF rollup DKRetail__TargetAmount__c / ActualAmount__c 재현).
            targetAmount: Long,
            actualAmount: Long
        ): PromotionDetailResponse {
            val productTypeName = promotion.productType
            // SF formula DKRetail__PromotionName__c = TEXT(DKRetail__ProductType__c) + '(' + DKRetail__PrimaryProductId__r.Name + ')'
            val promotionName = if (primaryProductName != null) {
                "${productTypeName.orEmpty()}(${primaryProductName})"
            } else null
            return PromotionDetailResponse(
                id = promotion.id,
                promotionNumber = promotion.promotionNumber,
                promotionName = promotionName,
                promotionType = promotion.promotionType?.displayName,
                accountId = promotion.account!!.id,
                accountName = accountName,
                accountCode = accountCode,
                startDate = promotion.startDate,
                endDate = promotion.endDate,
                primaryProductId = promotion.primaryProductId,
                primaryProductName = primaryProductName,
                primaryProductCode = primaryProductCode,
                otherProduct = promotion.otherProduct,
                message = promotion.message,
                standLocation = promotion.standLocation?.displayName,
                costCenterCode = promotion.costCenterCode,
                productType = productTypeName,
                category1 = promotion.category1,
                // SF rollup DKRetail__TargetAmount__c / ActualAmount__c = 조원 파생값 SUM 재현.
                // (동기화 스칼라 dkTargetAmount/dkActualAmount 는 갱신 로직 없어 stale 이므로 미사용.)
                targetAmount = targetAmount,
                actualAmount = actualAmount,
                isClosed = promotion.isClosed,
                isDeleted = promotion.isDeleted,
                createdById = promotion.createdBy?.id,
                createdByName = promotion.createdBy?.name,
                createdAt = promotion.createdAt,
                updatedAt = promotion.updatedAt,
                remark = promotion.remark
            )
        }
    }
}
