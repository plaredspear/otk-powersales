package com.otoki.powersales.sap.dto

data class SapAccountCategoryMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val accountCode: String? = null,
        val name: String? = null
    )
}
