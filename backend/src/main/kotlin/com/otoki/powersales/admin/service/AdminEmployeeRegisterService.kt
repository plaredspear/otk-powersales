package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterResponse
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.exception.InvalidEmployeeCodeFormatException
import com.otoki.powersales.admin.exception.PasswordConfirmMismatchException
import com.otoki.powersales.admin.util.AdminPasswordPolicyValidator
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 시스템 관리자 수동 등록 서비스 (Spec #579).
 *
 * - 호출자는 `UserRole.MANAGE_PERMISSIONS` 집합 (현재 SYSTEM_ADMIN 단일 원소) 에 속해야 한다.
 * - 등록되는 모든 직원은 `role = SYSTEM_ADMIN`, `origin = MANUAL`, `appLoginActive = false` 로 고정 저장된다.
 * - 비밀번호는 [AdminPasswordPolicyValidator] 검증 후 BCrypt 해시로 저장된다.
 */
@Service
@Transactional(readOnly = true)
class AdminEmployeeRegisterService(
    private val employeeRepository: EmployeeRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    private val logger = LoggerFactory.getLogger(AdminEmployeeRegisterService::class.java)

    /**
     * @param actor 호출자(controller) 가 주입한 인증 principal. role / employeeId / employeeCode snapshot 만 사용.
     */
    @Transactional
    fun register(actor: WebUserPrincipal, request: AdminEmployeeRegisterRequest): AdminEmployeeRegisterResponse {
        if (actor.role !in UserRoleEnum.MANAGE_PERMISSIONS) {
            throw AdminForbiddenException()
        }

        if (!EMPLOYEE_CODE_PATTERN.matches(request.employeeCode)) {
            throw InvalidEmployeeCodeFormatException()
        }

        if (request.password != request.passwordConfirm) {
            throw PasswordConfirmMismatchException()
        }

        AdminPasswordPolicyValidator.validate(request.password)

        if (employeeRepository.existsByEmployeeCode(request.employeeCode)) {
            throw EmployeeCodeDuplicatedException()
        }

        val encodedPassword = passwordEncoder.encode(request.password)!!
        val newEmployee = Employee(
            employeeCode = request.employeeCode,
            name = request.name,
            workEmail = request.workEmail,
            workPhone = request.workPhone,
            orgName = request.orgName,
            costCenterCode = request.costCenterCode,
            password = encodedPassword,
            passwordChangeRequired = true
        ).apply {
            role = UserRoleEnum.SYSTEM_ADMIN
            origin = EmployeeOrigin.MANUAL
            appLoginActive = false
        }

        val saved = try {
            employeeRepository.save(newEmployee)
        } catch (ex: DataIntegrityViolationException) {
            throw EmployeeCodeDuplicatedException()
        }

        logger.info(
            "ADMIN_ACCOUNT_REGISTERED actor={} actorCode={} target={} role=SYSTEM_ADMIN",
            actor.employeeId,
            actor.employeeCode,
            saved.employeeCode
        )

        return AdminEmployeeRegisterResponse.from(saved)
    }

    companion object {
        private val EMPLOYEE_CODE_PATTERN = Regex("^ADMIN-[A-Za-z0-9_-]{1,30}$")
    }
}
