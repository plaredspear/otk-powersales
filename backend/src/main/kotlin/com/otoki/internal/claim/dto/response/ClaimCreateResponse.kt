/*
package com.otoki.internal.claim.dto.response

import com.otoki.internal.claim.entity.Claim

/ **
 * 클레임 등록 응답 DTO
 * /
data class ClaimCreateResponse(
    val id: Long,
    val accountName: String,
    val accountId: Long,
    val productName: String,
    val productCode: String,
    val createdAt: String
) {
    companion object {
        fun from(claim: Claim): ClaimCreateResponse {
            return ClaimCreateResponse(
                id = claim.id,
                accountName = claim.accountName,
                accountId = claim.account.id,
                productName = claim.productName,
                productCode = claim.productCode,
                createdAt = claim.createdAt.toString()
            )
        }
    }
}
*/
