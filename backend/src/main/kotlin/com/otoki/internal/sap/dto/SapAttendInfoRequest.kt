package com.otoki.internal.sap.dto

data class SapAttendInfoRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val employeeCode: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
        val attendType: String? = null,
        val status: String? = null
    )
}
