package com.otoki.internal.sap.dto

data class SapMonthlySalesRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val sapAccountCode: String? = null,
        val salesYearMonth: String? = null,
        val abcClosingAmount1: String? = null,
        val abcClosingAmount2: String? = null,
        val abcClosingAmount3: String? = null,
        val totalLedgerAmount: String? = null,
        val shipClosingAmount: String? = null,
        val rlsales: String? = null
    )
}
