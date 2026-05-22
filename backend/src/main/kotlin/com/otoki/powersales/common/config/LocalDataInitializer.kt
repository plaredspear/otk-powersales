package com.otoki.powersales.common.config

import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.service.UserProvisioningService
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@Component
@Profile("local")
class LocalDataInitializer(
    private val employeeRepository: EmployeeRepository,
    private val userProvisioningService: UserProvisioningService,
    private val passwordEncoder: PasswordEncoder,
    private val agreementWordRepository: AgreementWordRepository,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runSafely("seedUser") { seedUser() }
        runSafely("seedSystemAdmin") { seedSystemAdmin() }
    }

    private fun runSafely(name: String, block: () -> Unit) {
        try {
            transactionTemplate.executeWithoutResult { block() }
        } catch (e: Exception) {
            log.warn("시드 데이터 '{}' 실행 실패 (기존 데이터 충돌 가능): {}", name, e.message)
        }
    }

    private fun employeeInfoExists(employeeCode: String): Boolean {
        val count = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM powersales.employee_info WHERE employee_code = :code"
        ).setParameter("code", employeeCode).singleResult as Number
        return count.toLong() > 0
    }

    private fun seedUser() {
        val encodedPassword = passwordEncoder.encode("1234")!!

        data class SeedEmployee(
            val code: String, val name: String, val role: UserRoleEnum,
            val orgName: String, val costCenterCode: String, val birthDate: String,
            val homePhone: String, val workPhone: String, val startDate: LocalDate,
            val workEmail: String
        )

        val seeds = listOf(
            // 로컬 개발 만능 계정 — 모든 권한(MANAGE_PERMISSIONS / ADMIN_GRADE / ALL_BRANCHES /
            // ALLOWED_FOR_ADMIN_LOGIN / Account 검증 우회 등) 분기가 SYSTEM_ADMIN 기준이라 단일 role 로 충족.
            SeedEmployee("99990001", "개발테스트", UserRoleEnum.SYSTEM_ADMIN, "테스트지점", "1111", "19850315", "02-1234-5678", "02-9876-5432", LocalDate.of(2015, 3, 1), "dev-test@otoki.local"),
            SeedEmployee("99990002", "여사원테스트", UserRoleEnum.WOMAN, "테스트지점", "1111", "19920820", "02-2345-6789", "02-8765-4321", LocalDate.of(2018, 7, 1), "woman-test@otoki.local"),
            SeedEmployee("99990003", "지점장테스트", UserRoleEnum.BRANCH_MANAGER, "테스트지점", "1111", "19780105", "02-3456-7890", "02-7654-3210", LocalDate.of(2010, 1, 15), "manager-test@otoki.local"),
            SeedEmployee("99990004", "강남조장", UserRoleEnum.LEADER, "테스트지점", "1111", "19880510", "02-4567-8901", "02-6543-2109", LocalDate.of(2016, 5, 1), "leader-test@otoki.local"),
            SeedEmployee("99990005", "강남여사원", UserRoleEnum.WOMAN, "강남지점", "1112", "19950320", "02-5678-9012", "02-5432-1098", LocalDate.of(2020, 3, 1), "woman-gn@otoki.local")
        )

        for (seed in seeds) {
            if (employeeRepository.existsByEmployeeCode(seed.code)) continue

            val infoExists = employeeInfoExists(seed.code)
            val employee = Employee(
                employeeCode = seed.code, name = seed.name, status = "재직",
                appLoginActive = true, orgName = seed.orgName, role = seed.role,
                birthDate = seed.birthDate, homePhone = seed.homePhone, workPhone = seed.workPhone,
                startDate = seed.startDate, costCenterCode = seed.costCenterCode,
                workEmail = seed.workEmail,
                password = encodedPassword, passwordChangeRequired = false
            )
            if (infoExists) {
                employee.employeeInfo = null
            }
            employeeRepository.save(employee)

            // SF 레거시 IF_REST_SAP_EmployeeMaster.upsertUser 동등 — Employee 신규 INSERT 시 매칭 User 동시 생성.
            // 운영 경로는 EmployeeUpsertService 가 EmployeeCreatedEvent 를 발행해 비동기 처리하지만,
            // 시드는 부트스트랩 단일 호출이라 트랜잭션 분리 의미가 없어 직접(동기) provision 호출.
            userProvisioningService.provisionForSeed(
                employeeCode = seed.code,
                name = seed.name,
                workEmail = seed.workEmail,
                email = null,
                birthDate = seed.birthDate,
                role = seed.role,
                appLoginActive = true,
                costCenterCode = seed.costCenterCode,
                encodedPassword = encodedPassword,
                passwordChangeRequired = false,
            )
            log.info("시드 계정 생성 완료: employeeCode={}, name={}", seed.code, seed.name)
        }
    }

    /**
     * Spec #579: Web Admin 수동 등록 SYSTEM_ADMIN 부트스트랩 시드.
     *
     * 등록 API (`POST /api/v1/admin/employees`) 의 호출자 권한 (`UserRole.MANAGE_PERMISSIONS`)
     * 을 충족하는 SYSTEM_ADMIN 계정을 생성한다. spec §1.2 정책에 따라 다음을 적용한다:
     * - `ADMIN-` prefix 사번
     * - `origin = MANUAL` (SAP 인바운드 갱신 보호 대상)
     * - `appLoginActive = false` (Web Admin 만 허용)
     * - SAP 미수신 필드(`status`, `birthDate`, `homePhone`, `workPhone`, `startDate`,
     *   `costCenterCode`)는 null
     */
    private fun seedSystemAdmin() {
        val encodedPassword = passwordEncoder.encode("1234")!!

        data class SeedSystemAdmin(val code: String, val name: String, val orgName: String, val workEmail: String)

        val seeds = listOf(
            SeedSystemAdmin("ADMIN-99999999", "시스템개발자", "시스템개발자조직", "sysadmin@otoki.local"),
            SeedSystemAdmin("ADMIN-99990001", "시스템개발자2", "시스템개발자조직", "sysadmin2@otoki.local")
        )

        for (seed in seeds) {
            if (employeeRepository.existsByEmployeeCode(seed.code)) continue

            val infoExists = employeeInfoExists(seed.code)
            val employee = Employee(
                employeeCode = seed.code,
                name = seed.name,
                orgName = seed.orgName,
                password = encodedPassword,
                passwordChangeRequired = false
            ).apply {
                role = UserRoleEnum.SYSTEM_ADMIN
                origin = EmployeeOrigin.MANUAL
                appLoginActive = false
                workEmail = seed.workEmail
            }
            if (infoExists) {
                employee.employeeInfo = null
            }
            employeeRepository.save(employee)

            // Web Admin 로그인 가능하도록 User 동시 생성 (시드는 동기 호출).
            userProvisioningService.provisionForSeed(
                employeeCode = seed.code,
                name = seed.name,
                workEmail = seed.workEmail,
                email = null,
                birthDate = null,
                role = UserRoleEnum.SYSTEM_ADMIN,
                appLoginActive = false,
                encodedPassword = encodedPassword,
                passwordChangeRequired = false,
            )
            log.info("시드 SYSTEM_ADMIN 계정 생성 완료: employeeCode={}", seed.code)
        }
    }
}
