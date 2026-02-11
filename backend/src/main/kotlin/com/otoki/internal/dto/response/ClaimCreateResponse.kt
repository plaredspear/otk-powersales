package com.otoki.internal.dto.response

import com.otoki.internal.entity.Claim

/**
 * 클레임 등록 응답 DTO
 */
data class ClaimCreateResponse(
    val id: Long,
    val storeName: String,
    val storeId: Long,
    val productName: String,
    val productCode: String,
    val createdAt: String
) {
    companion object {
        fun from(claim: Claim): ClaimCreateResponse {
            return ClaimCreateResponse(
                id = claim.id,
                storeName = claim.storeName,
                storeId = claim.store.id,
                productName = claim.productName,
                productCode = claim.productCode,
                createdAt = claim.createdAt.toString()
            )
        }
    }
}
