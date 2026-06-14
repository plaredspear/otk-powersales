package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.dto.AdminEmployeeRegisterResponse
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.exception.InvalidEmployeeCodeFormatException
import com.otoki.powersales.admin.exception.PasswordConfirmMismatchException
import com.otoki.powersales.admin.util.AdminPasswordPolicyValidator
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 시스템 관리자 수동 등록 서비스 (Spec #579).
 *
 * - 호출자는 `Profile.name == "시스템 관리자"` 여야 한다.
 * - 등록되는 모든 직원은 `role = null` (AppAuthority picklist 부재), `origin = MANUAL`, `appLoginActive = false` 로 고정 저장된다.
 *   (시스템 관리자 직무는 SF AppAuthority picklist 에 없음 — Profile.Name 으로만 표현)
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
        if (actor.profileName != SYSTEM_ADMIN_PROFILE_NAME) {
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
            // 시스템 관리자: SF AppAuthority picklist 부재 → role=null. Profile.Name 으로만 표현.
            role = null
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
        private const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"
    }
}
