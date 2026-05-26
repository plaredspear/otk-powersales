package com.otoki.powersales.common.config

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
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
import java.time.LocalDate

@Component
@Profile("local")
class LocalDataInitializer(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val userProvisioningService: UserProvisioningService,
    private val profileRepository: ProfileRepository,
    private val profileFlagsRepository: ProfileFlagsRepository,
    private val passwordEncoder: PasswordEncoder,
    private val transactionTemplate: TransactionTemplate,
    private val entityManager: EntityManager,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runSafely("seedProfiles") { seedProfiles() }
        runSafely("seedSystemAdminProfileFlags") { seedSystemAdminProfileFlags() }
        runSafely("seedUser") { seedUser() }
        runSafely("seedSystemAdmin") { seedSystemAdmin() }
    }

    /**
     * 로컬 환경 전용 Profile row 시드.
     *
     * dev/prod 는 SF Profile 마이그레이션 (Stage1 Profile 적재 + post-copy hook 의 SF Admin → '시스템 관리자'
     * rename) 이 profile 테이블을 채우므로 본 함수가 필요 없다. 로컬 환경에서는 SF 적재가 동작하지 않아 profile
     * 테이블이 비어있어, 후속 [seedUser] / [seedSystemAdmin] 의 profileNameForRole / SYSTEM_ADMIN_PROFILE_NAME
     * lookup 이 실패한다. [SystemAdminProfilePolicy.REQUIRED_PROFILE_NAMES] 12종을 name 기준으로 INSERT (sfid 는
     * null — 로컬에는 SF 가 없음).
     *
     * 멱등: name 으로 lookup 후 부재 시에만 INSERT.
     */
    private fun seedProfiles() {
        for (name in SystemAdminProfilePolicy.REQUIRED_PROFILE_NAMES) {
            if (profileRepository.findByName(name) == null) {
                profileRepository.save(com.otoki.powersales.auth.entity.Profile(name = name))
                log.info("[LocalDataInitializer] Profile 시드 — name='{}'", name)
            }
        }
    }

    /**
     * 로컬 환경 전용 — '시스템 관리자' Profile 의 ProfileFlags 5비트 TRUE 시드.
     *
     * dev/prod 는 Stage1 ProfileFlags 적재가 SF retrieve 의 systemPermissions 5비트를 그대로 적재하므로 본 함수가
     * 필요 없다. 로컬 환경에서는 SF 출처 CSV 가 없어 ProfileFlags 가 비어있어, [ProfileFlagsEvaluator] 가
     * `ProfileFlagsSnapshot.NONE` (전부 false) 를 반환해 web admin 진입 불가능.
     *
     * '시스템 관리자' Profile 1건만 시드 — 그 외 Profile 의 5비트는 false 유지 (로컬 권한 모의 운영).
     *
     * 멱등: 5비트 중 하나라도 false 면 모두 TRUE 로 update + INSERT.
     */
    private fun seedSystemAdminProfileFlags() {
        val profile = profileRepository.findByName(SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME) ?: return
        val flags = profileFlagsRepository.findByProfileId(profile.id) ?: ProfileFlags(profileId = profile.id)
        val needsUpdate = !flags.permissionsViewAllData || !flags.permissionsModifyAllData ||
            !flags.permissionsViewAllUsers || !flags.permissionsManageUsers || !flags.permissionsApiEnabled
        if (!needsUpdate) return
        flags.permissionsViewAllData = true
        flags.permissionsModifyAllData = true
        flags.permissionsViewAllUsers = true
        flags.permissionsManageUsers = true
        flags.permissionsApiEnabled = true
        profileFlagsRepository.save(flags)
        log.info(
            "[LocalDataInitializer] '{}' ProfileFlags 5비트 모두 TRUE 시드 — profile.id={}",
            SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME, profile.id,
        )
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
        val encodedPassword = passwordEncoder.encode("pwrs1234!")!!

        /**
         * `role`: SF DKRetail__AppAuthority__c picklist value 또는 null.
         *         시스템 관리자는 AppAuthority picklist 부재 → null.
         */
        data class SeedEmployee(
            val code: String, val name: String, val role: String?,
            val orgName: String, val costCenterCode: String, val birthDate: String,
            val homePhone: String, val workPhone: String, val startDate: LocalDate,
            val workEmail: String
        )

        val seeds = listOf(
            // 시스템 관리자: SF AppAuthority picklist 부재 → role=null. 권한은 Profile.Name="시스템 관리자" 로 표현.
            SeedEmployee("99990001", "개발테스트", null, "테스트지점", "1111", "19850315", "02-1234-5678", "02-9876-5432", LocalDate.of(2015, 3, 1), "dev-test@otoki.local"),
            SeedEmployee("99990002", "여사원테스트", AppAuthority.WOMAN, "테스트지점", "1111", "19920820", "02-2345-6789", "02-8765-4321", LocalDate.of(2018, 7, 1), "woman-test@otoki.local"),
            SeedEmployee("99990003", "지점장테스트", AppAuthority.BRANCH_MANAGER, "테스트지점", "1111", "19780105", "02-3456-7890", "02-7654-3210", LocalDate.of(2010, 1, 15), "manager-test@otoki.local"),
            SeedEmployee("99990004", "강남조장", AppAuthority.LEADER, "테스트지점", "1111", "19880510", "02-4567-8901", "02-6543-2109", LocalDate.of(2016, 5, 1), "leader-test@otoki.local"),
            SeedEmployee("99990005", "강남여사원", AppAuthority.WOMAN, "강남지점", "1112", "19950320", "02-5678-9012", "02-5432-1098", LocalDate.of(2020, 3, 1), "woman-gn@otoki.local")
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
            // 시스템 관리자 (seed.code=99990001) 는 role=null 이라 provisionForSeed 가 "5.영업사원" Profile 부여 →
            // 부적합. 명시적으로 시스템 관리자 Profile 부여하기 위해 별도 처리.
            val isSystemAdmin = seed.code == "99990001"
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
            if (isSystemAdmin) {
                val sysadminProfileId = profileRepository.findByName(SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)?.id
                val user = userRepository.findByEmployeeCode(seed.code)
                if (user != null && sysadminProfileId != null) {
                    user.profileId = sysadminProfileId
                    userRepository.save(user)
                }
            }
            log.info("시드 계정 생성 완료: employeeCode={}, name={}", seed.code, seed.name)
        }
    }

    /**
     * Web Admin 수동 등록 시스템 관리자 부트스트랩 시드.
     *
     * 등록 API (`POST /api/v1/admin/employees`) 의 호출자 권한 (Profile.name == "시스템 관리자")
     * 을 충족하는 계정을 생성한다. 다음 정책 적용:
     * - `ADMIN-` prefix 사번
     * - `origin = MANUAL` (SAP 인바운드 갱신 보호 대상)
     * - `appLoginActive = false` (Web Admin 만 허용)
     * - `role = null` (시스템 관리자는 SF AppAuthority picklist 부재)
     * - SAP 미수신 필드 (`status`, `birthDate`, ...) 는 null
     * - User.profileId 는 명시적으로 "시스템 관리자" Profile 지정.
     */
    private fun seedSystemAdmin() {
        val encodedPassword = passwordEncoder.encode("pwrs1234!")!!

        data class SeedSystemAdmin(val code: String, val name: String, val orgName: String, val workEmail: String)

        val seeds = listOf(
            SeedSystemAdmin("ADMIN-99999999", "시스템개발자", "시스템개발자조직", "sysadmin@otoki.local"),
            SeedSystemAdmin("ADMIN-99990001", "시스템개발자2", "시스템개발자조직", "sysadmin2@otoki.local")
        )

        val sysadminProfileId = profileRepository.findByName(SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)?.id

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
                role = null
                origin = EmployeeOrigin.MANUAL
                appLoginActive = false
                workEmail = seed.workEmail
            }
            if (infoExists) {
                employee.employeeInfo = null
            }
            employeeRepository.save(employee)

            userProvisioningService.provisionForSeed(
                employeeCode = seed.code,
                name = seed.name,
                workEmail = seed.workEmail,
                email = null,
                birthDate = null,
                role = null,
                appLoginActive = false,
                encodedPassword = encodedPassword,
                passwordChangeRequired = false,
            )
            // 시스템 관리자 Profile 강제 set — provisionForSeed 가 role=null → 5.영업사원 fallback 하기 때문에 후처리 필요.
            val user = userRepository.findByEmployeeCode(seed.code)
            if (user != null && sysadminProfileId != null) {
                user.profileId = sysadminProfileId
                userRepository.save(user)
            }

            log.info("시드 시스템 관리자 계정 생성 완료: employeeCode={}", seed.code)
        }
    }
}
