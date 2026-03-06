package com.otoki.internal.sap.dto

data class SapEmployeeMasterRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val employeeCode: String? = null,
        val employeeName: String? = null,
        val sex: String? = null,
        val homePhone: String? = null,
        val workPhone: String? = null,
        val workEmail: String? = null,
        val email: String? = null,
        val startDate: String? = null,
        val endDate: String? = null,
        val status: String? = null,
        val birthdate: String? = null,
        val orgCode: String? = null,
        val lockingFlag: String? = null
    )
}
