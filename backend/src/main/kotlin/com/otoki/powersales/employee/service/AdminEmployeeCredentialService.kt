package com.otoki.powersales.employee.service

import com.otoki.powersales.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사원 자격 정보(단말 UUID / 비밀번호) 운영자 리셋 서비스 (Spec #582 P1-B).
 *
 * - 단말 초기화: `Employee.resetDevice()` 호출 → `deviceUuid = null`
 * - 비밀번호 임시 리셋: 임시 비밀번호 `"1234"` 를 BCrypt 해시화하여 저장 + `passwordChangeRequired = true`
 *
 * 권한 검증은 컨트롤러 단의 `@RequiresPermission(EMPLOYEE_RESET_CREDENTIALS)` 가
 * `AdminAuthorityFilter` 에서 처리하며, 본 서비스는 진입 시점에 SYSTEM_ADMIN 권한 보유를 가정한다.
 */
@Service
@Transactional(readOnly = true)
class AdminEmployeeCredentialService(
    private val employeeRepository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(AdminEmployeeCredentialService::class.java)

    @Transactional
    fun resetDevice(employeeId: Long): ResetDeviceResponse {
        val employee = findActiveEmployee(employeeId)
        val previousDeviceBound = employee.deviceUuid != null
        employee.resetDevice()

        logger.info(
            "EMPLOYEE_DEVICE_RESET target={} employeeCode={} previousBound={}",
            employee.id,
            employee.employeeCode,
            previousDeviceBound
        )

        return ResetDeviceResponse.from(employee, previousDeviceBound)
    }

    @Transactional
    fun resetPassword(employeeId: Long): ResetPasswordResponse {
        val employee = findActiveEmployee(employeeId)
        val encoded = passwordEncoder.encode(TEMPORARY_PASSWORD)!!
        employee.resetPasswordToTemporary(encoded)

        logger.info(
            "EMPLOYEE_PASSWORD_RESET target={} employeeCode={}",
            employee.id,
            employee.employeeCode
        )

        return ResetPasswordResponse.from(employee)
    }

    private fun findActiveEmployee(employeeId: Long) =
        employeeRepository.findById(employeeId)
            .orElseThrow { EmployeeNotFoundException(employeeId) }
            .also {
                if (it.appLoginActive != true) throw EmployeeLoginInactiveException()
            }

    companion object {
        const val TEMPORARY_PASSWORD = "1234"
    }
}
