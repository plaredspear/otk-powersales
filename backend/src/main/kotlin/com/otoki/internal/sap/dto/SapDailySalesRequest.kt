package com.otoki.internal.sap.dto

data class SapDailySalesRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val sapAccountCode: String? = null,
        val salesDate: String? = null,
        val erpSalesAmount1: String? = null,
        val erpSalesAmount2: String? = null,
        val erpSalesAmount3: String? = null,
        val erpDistributionAmount1: String? = null,
        val erpDistributionAmount2: String? = null,
        val erpDistributionAmount3: String? = null,
        val ledgerAmount: String? = null
    )
}
