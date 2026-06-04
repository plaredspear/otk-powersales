package com.otoki.powersales.employee.dto.response

import com.otoki.powersales.employee.entity.Employee
import java.time.LocalDateTime

/**
 * 단말 초기화 (deviceUuid → null) 응답 DTO (Spec #582 P1-B §4.1).
 */
data class ResetDeviceResponse(
    val employeeId: Long,
    val employeeCode: String?,
    val name: String,
    val previousDeviceBound: Boolean,
    val resetAt: LocalDateTime
) {
    companion object {
        fun from(
            employee: Employee,
            previousDeviceBound: Boolean,
            resetAt: LocalDateTime = LocalDateTime.now()
        ): ResetDeviceResponse = ResetDeviceResponse(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            previousDeviceBound = previousDeviceBound,
            resetAt = resetAt
        )
    }
}

/**
 * 비밀번호 임시 리셋 응답 DTO (Spec #582 P1-B §4.2).
 *
 * 임시 비밀번호 평문은 응답에 포함하지 않는다.
 */
data class ResetPasswordResponse(
    val employeeId: Long,
    val employeeCode: String?,
    val name: String,
    val temporaryPasswordIssued: Boolean,
    val passwordChangeRequired: Boolean,
    val resetAt: LocalDateTime
) {
    companion object {
        fun from(
            employee: Employee,
            resetAt: LocalDateTime = LocalDateTime.now()
        ): ResetPasswordResponse = ResetPasswordResponse(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name,
            temporaryPasswordIssued = true,
            passwordChangeRequired = true,
            resetAt = resetAt
        )
    }
}
