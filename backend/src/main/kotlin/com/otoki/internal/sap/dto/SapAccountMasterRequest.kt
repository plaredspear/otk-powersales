package com.otoki.internal.sap.dto

data class SapAccountMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val accountCode: String? = null,
        val name: String? = null
    )
}
