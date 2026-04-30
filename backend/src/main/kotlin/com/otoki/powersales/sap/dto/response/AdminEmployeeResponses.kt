package com.otoki.powersales.sap.dto.response

import com.otoki.powersales.employee.entity.Employee

data class EmployeeListResponse(
    val content: List<EmployeeListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class EmployeeListItem(
    val id: Long,
    val employeeCode: String,
    val name: String,
    val status: String?,
    val gender: String?,
    val orgName: String?,
    val costCenterCode: String?,
    val appAuthority: String?,
    val startDate: String?,
    val endDate: String?,
    val appLoginActive: Boolean?,
    val workPhone: String?,
    val jikchak: String?,
    val jikwee: String?,
    val jikgub: String?,
    val jobCode: String?,
    val appointmentDate: String?,
    val ordDetailNode: String?
) {
    companion object {
        fun from(employee: Employee): EmployeeListItem = EmployeeListItem(
            id = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            status = employee.status,
            gender = employee.gender?.displayName,
            orgName = employee.orgName,
            costCenterCode = employee.costCenterCode,
            appAuthority = employee.appAuthority,
            startDate = employee.startDate?.toString(),
            endDate = employee.endDate?.toString(),
            appLoginActive = employee.appLoginActive,
            workPhone = employee.workPhone,
            jikchak = employee.jikchak,
            jikwee = employee.jikwee,
            jikgub = employee.jikgub,
            jobCode = employee.jobCode,
            appointmentDate = employee.appointmentDate?.toString(),
            ordDetailNode = employee.ordDetailNode
        )
    }
}
