package com.otoki.internal.admin.dto.response

data class AnnualLeaveDayDto(
    val date: String,
    val attendTypeName: String
)

data class EmployeeAnnualLeaveDto(
    val employeeCode: String,
    val employeeName: String,
    val orgName: String,
    val annualLeaveDays: List<AnnualLeaveDayDto>,
    val totalCount: Int
)
