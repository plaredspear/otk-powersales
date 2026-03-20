package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Employee

data class EmployeeListResponse(
    val content: List<EmployeeListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class EmployeeListItem(
    val employeeNumber: String,
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
        fun from(employee: Employee): EmployeeListItem = EmployeeListItem(
            employeeNumber = employee.employeeNumber,
            name = employee.name,
            status = employee.status,
            orgName = employee.orgName,
            costCenterCode = employee.costCenterCode,
            appAuthority = employee.appAuthority,
            startDate = employee.startDate?.toString(),
            appLoginActive = employee.appLoginActive,
            workPhone = employee.workPhone
        )
    }
}
