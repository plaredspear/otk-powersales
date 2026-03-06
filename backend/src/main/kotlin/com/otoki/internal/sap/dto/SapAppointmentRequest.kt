package com.otoki.internal.sap.dto

data class SapAppointmentRequest(
    val reqItemList: List<ReqItem> = emptyList()
) {
    data class ReqItem(
        val employeeCode: String? = null,
        val afterOrgCode: String? = null,
        val afterOrgName: String? = null,
        val jikchak: String? = null,
        val jikwee: String? = null,
        val jikgub: String? = null,
        val workType: String? = null,
        val manageType: String? = null,
        val jobCode: String? = null,
        val workArea: String? = null,
        val jikjong: String? = null,
        val appointDate: String? = null,
        val jobName: String? = null,
        val ordDetailCode: String? = null,
        val ordDetailNode: String? = null
    )
}
