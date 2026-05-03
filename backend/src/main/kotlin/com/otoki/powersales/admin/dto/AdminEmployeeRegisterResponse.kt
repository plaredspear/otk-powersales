package com.otoki.powersales.admin.dto

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.EmployeeOrigin
import java.time.LocalDateTime

/**
 * 시스템 관리자 수동 등록 응답 DTO (Spec #579).
 */
data class AdminEmployeeRegisterResponse(
    val employeeId: Long,
    val employeeCode: String,
    val name: String,
    val role: UserRole,
    val origin: EmployeeOrigin,
    val appLoginActive: Boolean,
    val passwordChangeRequired: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: Employee): AdminEmployeeRegisterResponse =
            AdminEmployeeRegisterResponse(
                employeeId = entity.id,
                employeeCode = entity.employeeCode,
                name = entity.name,
                role = entity.role ?: UserRole.SYSTEM_ADMIN,
                origin = entity.origin,
                appLoginActive = entity.appLoginActive ?: false,
                passwordChangeRequired = entity.passwordChangeRequired ?: true,
                createdAt = entity.createdAt
            )
    }
}
