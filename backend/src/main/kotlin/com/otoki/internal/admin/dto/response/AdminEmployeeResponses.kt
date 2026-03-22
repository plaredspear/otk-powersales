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
    val id: Long,
    val employeeNumber: String,
    val name: String,
    val status: String?,
    val sex: String?,
    val orgName: String?,
    val costCenterCode: String?,
    val appAuthority: String?,
    val startDate: String?,
    val endDate: String?,
    val appLoginActive: Boolean?,
    val workPhone: String?
) {
    companion object {
        fun from(employee: Employee): EmployeeListItem = EmployeeListItem(
            id = employee.id,
            employeeNumber = employee.employeeNumber,
            name = employee.name,
            status = employee.status,
            sex = employee.sex,
            orgName = employee.orgName,
            costCenterCode = employee.costCenterCode,
            appAuthority = employee.appAuthority,
            startDate = employee.startDate?.toString(),
            endDate = employee.endDate?.toString(),
            appLoginActive = employee.appLoginActive,
            workPhone = employee.workPhone
        )
    }
}
