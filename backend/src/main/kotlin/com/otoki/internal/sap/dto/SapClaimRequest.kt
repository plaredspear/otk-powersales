package com.otoki.internal.sap.dto

data class SapClaimRequest(
    val request: ClaimItem? = null
) {
    data class ClaimItem(
        val name: String? = null,
        val claimSequence: String? = null,
        val actionCode: String? = null,
        val claimStatus: String? = null,
        val content: String? = null,
        val reasonType: String? = null,
        val cosmosKey: String? = null
    )
}
