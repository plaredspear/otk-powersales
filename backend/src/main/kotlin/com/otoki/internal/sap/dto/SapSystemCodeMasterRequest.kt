package com.otoki.internal.sap.dto

data class SapSystemCodeMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val companyCode: String? = null,
        val groupCode: String? = null,
        val detailCode: String? = null,
        val groupCodeName: String? = null,
        val detailCodeName: String? = null,
        val seq: String? = null
    )
}
