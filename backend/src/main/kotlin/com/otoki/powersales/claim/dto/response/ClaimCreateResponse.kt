package com.otoki.powersales.claim.dto.response

import com.otoki.powersales.claim.entity.Claim

data class ClaimCreateResponse(
    val id: Long,
    val accountName: String?,
    val accountId: Int?,
    val productName: String?,
    val productCode: String?,
    val createdAt: String
) {
    companion object {
        fun from(claim: Claim): ClaimCreateResponse = ClaimCreateResponse(
            id = claim.id,
            accountName = claim.account?.name,
            accountId = claim.account?.id,
            productName = claim.product?.name,
            productCode = claim.product?.productCode,
            createdAt = claim.createdAt.toString()
        )
    }
}
