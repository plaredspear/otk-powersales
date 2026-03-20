package com.otoki.internal.sap.dto

data class SapAccountMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val sapAccountCode: String? = null,
        val accountType: String? = null,
        val name: String? = null,
        val accountStatusName: String? = null,
        val accountGroup: String? = null,
        val phone: String? = null,
        val mobilePhone: String? = null,
        val employeeCode: String? = null,
        val representative: String? = null,
        val zipcode: String? = null,
        val address1: String? = null,
        val address2: String? = null,
        val branchCode: String? = null,
        val branchName: String? = null,
        val closingTime1: String? = null,
        val closingTime2: String? = null,
        val closingTime3: String? = null,
        val abcType: String? = null,
        val abcTypeCode: String? = null,
        val distribution: String? = null,
        val werk1Tx: String? = null,
        val werk2Tx: String? = null,
        val werk3Tx: String? = null
    )
}
