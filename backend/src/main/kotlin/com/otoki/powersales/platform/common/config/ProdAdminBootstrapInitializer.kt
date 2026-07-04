package com.otoki.powersales.platform.common.config

import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.user.service.UserProvisioningService
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * prod 환경 시스템 관리자 부트스트랩 시드.
 *
 * 신규 prod DB 최초 기동 시, Web Admin 등록 API (`POST /api/v1/admin/employees`) 호출자 권한
 * (Profile.name == "시스템 관리자") 을 충족하는 단일 부트스트랩 계정을 생성한다. 이 계정으로 로그인하여
 * 운영 시스템 관리자 / 일반 사용자를 등록한다.
 *
 * local 전용 [LocalDataInitializer] 와의 차이:
 * - prod 에서는 sysadmin@otoki.local (ADMIN-99999999) **단 1개만** 생성 (테스트 사번 99990001~5 미생성).
 * - 초기 비밀번호 `pwrs1234!` 는 동일하나 `passwordChangeRequired = true` — 최초 로그인 시 변경 강제.
 *
 * [LocalDataInitializer.seedSystemAdmin] 의 ADMIN- 시스템 관리자 정책을 그대로 따른다:
 * - `ADMIN-` prefix 사번
 * - `origin = MANUAL` (SAP 인바운드 갱신 보호 대상)
 * - `appLoginActive = false` (Web Admin 만 허용)
 * - `role = null` (시스템 관리자는 SF AppAuthority picklist 부재)
 * - SAP 미수신 필드 (`status`, `birthDate`, ...) 는 null
 * - User.profileId 는 명시적으로 "시스템 관리자" Profile 지정.
 *
 * Profile row 자체의 SoT 는 SF Profile 마이그레이션 (Stage1 적재) 이며, 본 시드는 이미 적재된
 * "시스템 관리자" Profile 을 참조한다. Profile 부재 시 profileId 는 null 로 두고 (Stage1 적재 후
 * web admin 에서 따로 연결), 멱등성을 위해 동일 사번이 이미 있으면 skip 한다.
 */
@Component
@Profile("prod")
class ProdAdminBootstrapInitializer(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val userProvisioningService: UserProvisioningService,
    private val profileRepository: ProfileRepository,
    private val passwordEncoder: PasswordEncoder,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val ADMIN_EMPLOYEE_CODE = "ADMIN-99999999"
        const val ADMIN_NAME = "시스템개발자"
        const val ADMIN_ORG_NAME = "시스템개발자조직"
        const val ADMIN_WORK_EMAIL = "sysadmin@otoki.local"
    }

    override fun run(args: ApplicationArguments) {
        try {
            transactionTemplate.executeWithoutResult { seedProdSystemAdmin() }
        } catch (e: Exception) {
            log.warn("prod 시스템 관리자 부트스트랩 시드 실행 실패 (기존 데이터 충돌 가능): {}", e.message)
        }
    }

    private fun employeeInfoExists(employeeCode: String): Boolean {
        val count = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM powersales.employee_info WHERE employee_code = :code"
        ).setParameter("code", employeeCode).singleResult as Number
        return count.toLong() > 0
    }

    private fun seedProdSystemAdmin() {
        if (employeeRepository.existsByEmployeeCode(ADMIN_EMPLOYEE_CODE)) {
            log.info("prod 시스템 관리자 계정 이미 존재 — 시드 skip: employeeCode={}", ADMIN_EMPLOYEE_CODE)
            return
        }

        val encodedPassword = passwordEncoder.encode("pwrs1234!")!!
        val sysadminProfileId = profileRepository.findByName(SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)?.id

        val infoExists = employeeInfoExists(ADMIN_EMPLOYEE_CODE)
        val employee = Employee(
            employeeCode = ADMIN_EMPLOYEE_CODE,
            name = ADMIN_NAME,
            orgName = ADMIN_ORG_NAME,
            password = encodedPassword,
            passwordChangeRequired = true,
        ).apply {
            role = null
            origin = EmployeeOrigin.MANUAL
            appLoginActive = false
            workEmail = ADMIN_WORK_EMAIL
        }
        if (infoExists) {
            employee.employeeInfo = null
        }
        employeeRepository.save(employee)

        userProvisioningService.provisionForSeed(
            employeeCode = ADMIN_EMPLOYEE_CODE,
            name = ADMIN_NAME,
            workEmail = ADMIN_WORK_EMAIL,
            email = null,
            birthDate = null,
            role = null,
            appLoginActive = false,
            encodedPassword = encodedPassword,
            passwordChangeRequired = true,
        )
        // 시스템 관리자 Profile 강제 set — provisionForSeed 가 role=null → 5.영업사원 fallback 하기 때문에 후처리 필요.
        // isActive 강제 set — provision 이 웹 게이트(isActive)를 모바일 게이트(appLoginActive=false)로 흘려보내
        // 웹 관리자 로그인마저 막히므로, 웹 전용 계정인 시스템 관리자는 웹 로그인을 열어준다 (모바일은 appLoginActive=false 유지).
        val user = userRepository.findByEmployeeCode(ADMIN_EMPLOYEE_CODE)
        if (user != null) {
            if (sysadminProfileId != null) {
                user.profileId = sysadminProfileId
            }
            user.isActive = true
            userRepository.save(user)
        }

        log.info("prod 시스템 관리자 부트스트랩 계정 생성 완료: employeeCode={}", ADMIN_EMPLOYEE_CODE)
    }
}
