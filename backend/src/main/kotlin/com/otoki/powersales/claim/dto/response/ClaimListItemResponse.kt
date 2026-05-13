package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim
import java.time.LocalDateTime

data class ClaimListItemResponse(
    val claimId: Long,
    val accountName: String?,
    val productName: String?,
    val productCode: String?,
    val categoryName: String?,
    val subcategoryName: String?,
    val defectQuantity: Int?,
    val status: String,
    val statusLabel: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(claim: Claim): ClaimListItemResponse = ClaimListItemResponse(
            claimId = claim.id,
            accountName = claim.accountName,
            productName = claim.productName,
            productCode = claim.productCode,
            categoryName = claim.category.name,
            subcategoryName = claim.subcategory.name,
            defectQuantity = claim.defectQuantity,
            status = claim.status.name,
            statusLabel = claim.status.displayName,
            createdAt = claim.createdAt
        )
    }
}
