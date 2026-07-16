package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.domain.org.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.domain.org.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.common.security.ActiveDeviceStore
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사원 자격 정보(단말 UUID / 비밀번호) 운영자 리셋 서비스 (Spec #582 P1-B).
 *
 * - 단말 초기화: `Employee.resetDevice()` 호출 → `deviceUuid = null`
 * - 비밀번호 임시 리셋: 임시 비밀번호 `"pwrs1234!"` 를 BCrypt 해시화하여 저장 + `passwordChangeRequired = true`
 *
 * 권한 검증은 컨트롤러 단의 `@RequiresPermission(EMPLOYEE_RESET_CREDENTIALS)` 가
 * `WebAdminContextFilter` 에서 처리하며, 본 서비스는 진입 시점에 SYSTEM_ADMIN 권한 보유를 가정한다.
 */
@Service
@Transactional(readOnly = true)
class AdminEmployeeCredentialService(
    private val employeeRepository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
    private val activeDeviceStore: ActiveDeviceStore,
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val logger = LoggerFactory.getLogger(AdminEmployeeCredentialService::class.java)

    @Transactional
    fun resetDevice(employeeId: Long): ResetDeviceResponse {
        val employee = findActiveEmployee(employeeId)
        val previousDeviceBound = employee.deviceUuid != null
        employee.resetDevice()

        // 기존 기기 즉시 차단: access token 활성기기 캐시 제거 + refresh token 무효화
        // (레거시 AuthService.resetDevice 동등). Redis/토큰 장애가 DB 초기화 트랜잭션을 막지 않도록 예외 삼킴.
        try {
            activeDeviceStore.clearActiveDevice(employee.id)
            jwtTokenProvider.deleteRefreshTokenByUserId(employee.id)
        } catch (e: Exception) {
            logger.warn("단말 초기화 중 토큰/캐시 정리 실패(무시): employeeCode={}", employee.employeeCode, e)
        }

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
        const val TEMPORARY_PASSWORD = "pwrs1234!"
    }
}
