package com.otoki.internal.sap.dto

data class SapClaimResponse(
    val resultCode: String,
    val resultMsg: String
) {
    companion object {
        fun success() = SapClaimResponse("S", "성공")
        fun error(message: String) = SapClaimResponse("E", message)
    }
}
