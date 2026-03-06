package com.otoki.internal.sap.dto

data class SapSyncResponse(
    val resultCode: String,
    val resultMsg: String
) {
    companion object {
        fun success() = SapSyncResponse("200", "SUCCESS")
        fun empty() = SapSyncResponse("200", "EMPTY_REQUEST")
        fun error(message: String) = SapSyncResponse("0", message)
    }
}

data class SapSyncResult(
    val successCount: Int,
    val failCount: Int,
    val errors: List<SapSyncError> = emptyList()
)

data class SapSyncError(
    val index: Int,
    val field: String?,
    val value: String?,
    val error: String
)
