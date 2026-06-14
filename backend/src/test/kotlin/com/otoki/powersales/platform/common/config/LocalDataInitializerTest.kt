package com.otoki.powersales.platform.common.config

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.common.config.LocalDataInitializer
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.user.service.UserProvisioningService
import io.mockk.*
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer

@DisplayName("LocalDataInitializer 테스트")
class LocalDataInitializerTest {

    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)

    private val userRepository: UserRepository = mockk(relaxed = true)

    private val userProvisioningService: UserProvisioningService = mockk(relaxed = true)

    private val profileRepository: ProfileRepository = mockk(relaxed = true)

    private val passwordEncoder: PasswordEncoder = mockk(relaxed = true)

    private val transactionTemplate: TransactionTemplate = mockk()

    private val entityManager: EntityManager = mockk()

    private val savedEmployees = mutableListOf<Employee>()

    private val localDataInitializer = LocalDataInitializer(
        employeeRepository,
        userRepository,
        userProvisioningService,
        profileRepository,
        passwordEncoder,
        transactionTemplate,
        entityManager,
    )

    @BeforeEach
    fun setUp() {
        savedEmployees.clear()
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            val callback = firstArg<Consumer<TransactionStatus>>()
            callback.accept(mockk<TransactionStatus>(relaxed = true))
            null
        }
        // relaxed mockk 는 User? 의 generic 추론 실패로 Object 반환 → ClassCastException.
        // 명시적 null stub 으로 우회.
        every { userRepository.findByEmployeeCode(any()) } returns null
        every {
            userProvisioningService.provisionForSeed(
                employeeCode = any(),
                name = any(),
                workEmail = any(),
                email = any(),
                birthDate = any(),
                role = any(),
                appLoginActive = any(),
                costCenterCode = any(),
                isSalesSupport = any(),
                encodedPassword = any(),
                passwordChangeRequired = any(),
            )
        } just Runs
    }

    private fun stubEmployeeInfoExists() {
        val mockQuery = mockk<Query>(relaxed = true)
        every { entityManager.createNativeQuery(any<String>()) } returns mockQuery
        every { mockQuery.setParameter(any<String>(), any()) } returns mockQuery
        every { mockQuery.singleResult } returns 0L
    }

    private fun stubAllUsersNotExist() {
        every { employeeRepository.existsByEmployeeCode("99990001") } returns false
        every { employeeRepository.existsByEmployeeCode("99990002") } returns false
        every { employeeRepository.existsByEmployeeCode("99990003") } returns false
        every { employeeRepository.existsByEmployeeCode("99990004") } returns false
        every { employeeRepository.existsByEmployeeCode("99990005") } returns false
        every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns false
        every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns false
        every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
        every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
        every { employeeRepository.save(any<Employee>()) } answers {
            val emp = firstArg<Employee>()
            savedEmployees.add(emp)
            emp
        }
        stubEmployeeInfoExists()
    }

    private fun stubAllUsersExist() {
        every { employeeRepository.existsByEmployeeCode("99990001") } returns true
        every { employeeRepository.existsByEmployeeCode("99990002") } returns true
        every { employeeRepository.existsByEmployeeCode("99990003") } returns true
        every { employeeRepository.existsByEmployeeCode("99990004") } returns true
        every { employeeRepository.existsByEmployeeCode("99990005") } returns true
        every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns true
        every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns true
    }

    private fun captureAllSavedEmployees(): List<Employee> {
        verify(exactly = 7) { employeeRepository.save(any<Employee>()) }
        return savedEmployees.toList()
    }

    @Nested
    @DisplayName("seedUser - 만능 개발 계정 생성")
    inner class LeaderUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000001 없음 -> 만능 개발 계정 필드 검증")
        fun createsLeaderUser_whenNotExists() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val leader = employees.find { it.employeeCode == "99990001" }!!
            assertThat(leader.name).isEqualTo("개발테스트")
            assertThat(leader.status).isEqualTo("재직")
            assertThat(leader.appLoginActive).isTrue()
            assertThat(leader.orgName).isEqualTo("테스트지점")
            assertThat(leader.role).isEqualTo(null)
            assertThat(leader.password).isEqualTo("encoded_password")
            assertThat(leader.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 만능 개발 계정 -> UserRole.SYSTEM_ADMIN")
        fun leaderUser_hasSalesSupportRole() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val leader = employees.find { it.employeeCode == "99990001" }!!
            assertThat(leader.role).isEqualTo(null)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000001 존재 -> save 미호출")
        fun skipsLeader_whenAlreadyExists() {
            stubAllUsersExist()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 0) { employeeRepository.save(any<Employee>()) }
        }
    }

    @Nested
    @DisplayName("seedUser - 여사원 사용자 생성")
    inner class SalesUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000002 없음 -> 여사원 사용자 필드 검증")
        fun createsSalesUser_whenNotExists() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val salesUser = employees.find { it.employeeCode == "99990002" }!!
            assertThat(salesUser.name).isEqualTo("여사원테스트")
            assertThat(salesUser.status).isEqualTo("재직")
            assertThat(salesUser.appLoginActive).isTrue()
            assertThat(salesUser.orgName).isEqualTo("테스트지점")
            assertThat(salesUser.role).isEqualTo(AppAuthority.WOMAN)
            assertThat(salesUser.password).isEqualTo("encoded_password")
            assertThat(salesUser.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 여사원 사용자 -> UserRole.WOMAN")
        fun salesUser_hasUserRole() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val salesUser = employees.find { it.employeeCode == "99990002" }!!
            assertThat(salesUser.role).isEqualTo(AppAuthority.WOMAN)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000002 존재 -> 해당 사용자 save 미호출")
        fun skipsSalesUser_whenAlreadyExists() {
            every { employeeRepository.existsByEmployeeCode("99990001") } returns false
            every { employeeRepository.existsByEmployeeCode("99990002") } returns true
            every { employeeRepository.existsByEmployeeCode("99990003") } returns false
            every { employeeRepository.existsByEmployeeCode("99990004") } returns false
            every { employeeRepository.existsByEmployeeCode("99990005") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns false
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { employeeRepository.save(any<Employee>()) } answers { val emp = firstArg<Employee>(); savedEmployees.add(emp); emp }
            stubEmployeeInfoExists()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 6) { employeeRepository.save(any<Employee>()) }
            val savedIds = savedEmployees.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("99990002")
        }
    }

    @Nested
    @DisplayName("seedUser - 지점장 사용자 생성")
    inner class AdminUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000003 없음 -> 지점장 사용자 필드 검증")
        fun createsAdminUser_whenNotExists() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val admin = employees.find { it.employeeCode == "99990003" }!!
            assertThat(admin.name).isEqualTo("지점장테스트")
            assertThat(admin.status).isEqualTo("재직")
            assertThat(admin.appLoginActive).isTrue()
            assertThat(admin.orgName).isEqualTo("테스트지점")
            assertThat(admin.role).isEqualTo(AppAuthority.BRANCH_MANAGER)
            assertThat(admin.password).isEqualTo("encoded_password")
            assertThat(admin.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 지점장 사용자 -> UserRole.BRANCH_MANAGER")
        fun adminUser_hasAdminRole() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val admin = employees.find { it.employeeCode == "99990003" }!!
            assertThat(admin.role).isEqualTo(AppAuthority.BRANCH_MANAGER)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000003 존재 -> 해당 사용자 save 미호출")
        fun skipsAdminUser_whenAlreadyExists() {
            every { employeeRepository.existsByEmployeeCode("99990001") } returns false
            every { employeeRepository.existsByEmployeeCode("99990002") } returns false
            every { employeeRepository.existsByEmployeeCode("99990003") } returns true
            every { employeeRepository.existsByEmployeeCode("99990004") } returns false
            every { employeeRepository.existsByEmployeeCode("99990005") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns false
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { employeeRepository.save(any<Employee>()) } answers { val emp = firstArg<Employee>(); savedEmployees.add(emp); emp }
            stubEmployeeInfoExists()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 6) { employeeRepository.save(any<Employee>()) }
            val savedIds = savedEmployees.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("99990003")
        }
    }

    @Nested
    @DisplayName("seedSystemAdmin - 시스템관리자 부트스트랩 시드 (Spec #579)")
    inner class SystemAdminUserTests {

        @Test
        @DisplayName("정상 생성 - ADMIN-99999999 없음 -> ADMIN- prefix + role/origin/appLoginActive 정책 준수")
        fun createsSystemAdminUser_whenNotExists() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val sysAdmin = employees.find { it.employeeCode == "ADMIN-99999999" }!!
            assertThat(sysAdmin.employeeCode).startsWith("ADMIN-")
            assertThat(sysAdmin.name).isEqualTo("시스템개발자")
            assertThat(sysAdmin.role).isEqualTo(null)
            assertThat(sysAdmin.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(sysAdmin.appLoginActive).isFalse()
            assertThat(sysAdmin.orgName).isEqualTo("시스템개발자조직")
            assertThat(sysAdmin.password).isEqualTo("encoded_password")
            assertThat(sysAdmin.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("SAP 미수신 필드 null - 수동 등록은 SAP 마스터에 존재하지 않음")
        fun systemAdmin_hasNullSapOnlyFields() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val sysAdmin = employees.find { it.employeeCode == "ADMIN-99999999" }!!
            assertThat(sysAdmin.status).isNull()
            assertThat(sysAdmin.birthDate).isNull()
            assertThat(sysAdmin.homePhone).isNull()
            assertThat(sysAdmin.workPhone).isNull()
            assertThat(sysAdmin.startDate).isNull()
            assertThat(sysAdmin.costCenterCode).isNull()
        }

        @Test
        @DisplayName("시스템 관리자 시드 — Employee.role 은 null (SF AppAuthority picklist 부재)")
        fun systemAdmin_hasNullRole() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val sysAdmin = employees.find { it.employeeCode == "ADMIN-99999999" }!!
            assertThat(sysAdmin.role).isNull()
        }

        @Test
        @DisplayName("정상 생성 - ADMIN-99990001 두 번째 SYSTEM_ADMIN 도 동일 정책 적용")
        fun createsSecondSystemAdmin_whenNotExists() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val sysAdmin2 = employees.find { it.employeeCode == "ADMIN-99990001" }!!
            assertThat(sysAdmin2.employeeCode).startsWith("ADMIN-")
            assertThat(sysAdmin2.name).isEqualTo("시스템개발자2")
            assertThat(sysAdmin2.role).isEqualTo(null)
            assertThat(sysAdmin2.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(sysAdmin2.appLoginActive).isFalse()
            assertThat(sysAdmin2.orgName).isEqualTo("시스템개발자조직")
            assertThat(sysAdmin2.password).isEqualTo("encoded_password")
            assertThat(sysAdmin2.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("멱등성 - ADMIN-99999999 / ADMIN-99990001 모두 존재 -> 해당 사용자 save 미호출")
        fun skipsSystemAdmin_whenAlreadyExists() {
            every { employeeRepository.existsByEmployeeCode("99990001") } returns false
            every { employeeRepository.existsByEmployeeCode("99990002") } returns false
            every { employeeRepository.existsByEmployeeCode("99990003") } returns false
            every { employeeRepository.existsByEmployeeCode("99990004") } returns false
            every { employeeRepository.existsByEmployeeCode("99990005") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns true
            every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns true
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { employeeRepository.save(any<Employee>()) } answers { val emp = firstArg<Employee>(); savedEmployees.add(emp); emp }
            stubEmployeeInfoExists()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 5) { employeeRepository.save(any<Employee>()) }
            val savedIds = savedEmployees.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("ADMIN-99999999", "ADMIN-99990001")
        }
    }

    @Nested
    @DisplayName("seedUser - 부분 존재 및 동일 지점 검증")
    inner class PartialAndGroupTests {

        @Test
        @DisplayName("부분 존재 - 00000001만 존재 -> 나머지 6명만 생성")
        fun createsOnlyMissing_whenPartiallyExists() {
            every { employeeRepository.existsByEmployeeCode("99990001") } returns true
            every { employeeRepository.existsByEmployeeCode("99990002") } returns false
            every { employeeRepository.existsByEmployeeCode("99990003") } returns false
            every { employeeRepository.existsByEmployeeCode("99990004") } returns false
            every { employeeRepository.existsByEmployeeCode("99990005") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99999999") } returns false
            every { employeeRepository.existsByEmployeeCode("ADMIN-99990001") } returns false
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { passwordEncoder.encode("pwrs1234!") } returns "encoded_password"
            every { employeeRepository.save(any<Employee>()) } answers { val emp = firstArg<Employee>(); savedEmployees.add(emp); emp }
            stubEmployeeInfoExists()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 6) { employeeRepository.save(any<Employee>()) }
            val savedIds = savedEmployees.map { it.employeeCode }
            assertThat(savedIds).containsExactly("99990002", "99990003", "99990004", "99990005", "ADMIN-99999999", "ADMIN-99990001")
        }

        @Test
        @DisplayName("테스트지점 소속 검증 - 00000001~00000004 테스트지점, ADMIN-* 시스템개발자조직")
        fun testBranchUsers() {
            stubAllUsersNotExist()

            localDataInitializer.run(DefaultApplicationArguments())

            val employees = captureAllSavedEmployees()
            val testBranchEmployees = employees.filter { it.orgName == "테스트지점" }
            assertThat(testBranchEmployees).hasSize(4)
            assertThat(testBranchEmployees.map { it.employeeCode })
                .containsExactlyInAnyOrder("99990001", "99990002", "99990003", "99990004")
            val headOfficeEmployees = employees.filter { it.orgName == "시스템개발자조직" }
            assertThat(headOfficeEmployees).hasSize(2)
            assertThat(headOfficeEmployees.map { it.employeeCode })
                .containsExactlyInAnyOrder("ADMIN-99999999", "ADMIN-99990001")
        }

        @Test
        @DisplayName("전체 멱등성 - 일곱 사용자 모두 존재 -> save 미호출")
        fun noSave_whenAllExist() {
            stubAllUsersExist()

            localDataInitializer.run(DefaultApplicationArguments())

            verify(exactly = 0) { employeeRepository.save(any<Employee>()) }
        }
    }
}
