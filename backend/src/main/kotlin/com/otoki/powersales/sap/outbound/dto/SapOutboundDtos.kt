package com.otoki.powersales.sap.outbound.dto

data class SapOutboundRequest<T>(
    val interfaceId: String,
    val reqItemList: List<T>
)

data class SapOutboundResponse(
    val resultCode: String,
    val resultMsg: String
) {
    fun isSuccess(): Boolean = resultCode == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE: String = "200"
        const val FAIL_CODE: String = "0"
    }
}
