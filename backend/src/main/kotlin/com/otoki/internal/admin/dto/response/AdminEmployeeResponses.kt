package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.User

data class EmployeeListResponse(
    val content: List<EmployeeListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class EmployeeListItem(
    val employeeId: String,
    val name: String,
    val status: String?,
    val orgName: String?,
    val costCenterCode: String?,
    val appAuthority: String?,
    val startDate: String?,
    val appLoginActive: Boolean?,
    val workPhone: String?
) {
    companion object {
        fun from(user: User): EmployeeListItem = EmployeeListItem(
            employeeId = user.employeeId,
            name = user.name,
            status = user.status,
            orgName = user.orgName,
            costCenterCode = user.costCenterCode,
            appAuthority = user.appAuthority,
            startDate = user.startDate?.toString(),
            appLoginActive = user.appLoginActive,
            workPhone = user.workPhone
        )
    }
}
