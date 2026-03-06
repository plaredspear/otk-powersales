package com.otoki.internal.sap.dto

data class SapBarcodeMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val productCode: String? = null,
        val productName: String? = null,
        val productUnit: String? = null,
        val productSequence: String? = null,
        val productBarcode: String? = null
    )
}
