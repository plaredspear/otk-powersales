package com.otoki.powersales.sap.outbound.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class SapOutboundRequest<T>(
    @JsonIgnore val interfaceId: String,
    @JsonProperty("REQUEST") val reqItemList: List<T>
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
