package com.otoki.powersales.common.config

import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.promotion.repository.PromotionTypeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.BeforeEach
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.support.TransactionTemplate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LocalDataInitializer 테스트")
class LocalDataInitializerTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var agreementWordRepository: AgreementWordRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var promotionTypeRepository: PromotionTypeRepository

    @Mock
    private lateinit var transactionTemplate: TransactionTemplate

    @Mock
    private lateinit var entityManager: EntityManager

    @InjectMocks
    private lateinit var localDataInitializer: LocalDataInitializer

    @BeforeEach
    fun setUp() {
        whenever(transactionTemplate.executeWithoutResult(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>(0)
            callback.accept(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus::class.java))
            null
        }
    }

    private fun stubEmployeeInfoExists() {
        val mockQuery = org.mockito.Mockito.mock(Query::class.java)
        whenever(entityManager.createNativeQuery(any<String>())).thenReturn(mockQuery)
        whenever(mockQuery.setParameter(any<String>(), any())).thenReturn(mockQuery)
        whenever(mockQuery.singleResult).thenReturn(0L)
    }

    private fun stubAllUsersNotExist() {
        whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(false)
        whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(false)
        whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(false)
        whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(false)
        whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(false)
        whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(false)
        whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
        whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
        stubEmployeeInfoExists()
    }

    private fun stubAllUsersExist() {
        whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(true)
        whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(true)
        whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(true)
        whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(true)
        whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(true)
        whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(true)
    }

    private fun stubAllAccountsExist() {
        for (i in 1..8) {
            whenever(accountRepository.findByExternalKey("TEST-ACC-%03d".format(i)))
                .thenReturn(Account(externalKey = "TEST-ACC-%03d".format(i)))
        }
    }

    private fun stubAllAccountsNotExist() {
        for (i in 1..8) {
            whenever(accountRepository.findByExternalKey("TEST-ACC-%03d".format(i)))
                .thenReturn(null)
        }
        whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }
    }

    private fun stubOtherSeedsExist() {
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.of(AgreementWord()))
        whenever(organizationRepository.count()).thenReturn(1L)
        stubAllAccountsExist()
        whenever(promotionTypeRepository.count()).thenReturn(1L)
    }

    private fun captureAllSavedEmployees(): List<Employee> {
        val captor = argumentCaptor<Employee>()
        verify(employeeRepository, times(6)).save(captor.capture())
        return captor.allValues
    }

    @Nested
    @DisplayName("seedUser - 영업지원실 사용자 생성")
    inner class LeaderUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000001 없음 -> 영업지원실 사용자 필드 검증")
        fun createsLeaderUser_whenNotExists() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val leader = employees.find { it.employeeCode == "99990001" }!!
            assertThat(leader.name).isEqualTo("개발테스트")
            assertThat(leader.status).isEqualTo("재직")
            assertThat(leader.appLoginActive).isTrue()
            assertThat(leader.orgName).isEqualTo("테스트지점")
            assertThat(leader.role).isEqualTo(UserRole.SALES_SUPPORT)
            assertThat(leader.password).isEqualTo("encoded_password")
            assertThat(leader.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 영업지원실 사용자 -> UserRole.SALES_SUPPORT")
        fun leaderUser_hasSalesSupportRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val leader = employees.find { it.employeeCode == "99990001" }!!
            assertThat(leader.role).isEqualTo(UserRole.SALES_SUPPORT)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000001 존재 -> save 미호출")
        fun skipsLeader_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(employeeRepository, never()).save(any<Employee>())
        }
    }

    @Nested
    @DisplayName("seedUser - 여사원 사용자 생성")
    inner class SalesUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000002 없음 -> 여사원 사용자 필드 검증")
        fun createsSalesUser_whenNotExists() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val salesUser = employees.find { it.employeeCode == "99990002" }!!
            assertThat(salesUser.name).isEqualTo("여사원테스트")
            assertThat(salesUser.status).isEqualTo("재직")
            assertThat(salesUser.appLoginActive).isTrue()
            assertThat(salesUser.orgName).isEqualTo("테스트지점")
            assertThat(salesUser.role).isEqualTo(UserRole.WOMAN)
            assertThat(salesUser.password).isEqualTo("encoded_password")
            assertThat(salesUser.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 여사원 사용자 -> UserRole.WOMAN")
        fun salesUser_hasUserRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val salesUser = employees.find { it.employeeCode == "99990002" }!!
            assertThat(salesUser.role).isEqualTo(UserRole.WOMAN)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000002 존재 -> 해당 사용자 save 미호출")
        fun skipsSalesUser_whenAlreadyExists() {
            // Given
            whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(true)
            whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            stubEmployeeInfoExists()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val captor = argumentCaptor<Employee>()
            verify(employeeRepository, times(5)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("99990002")
        }
    }

    @Nested
    @DisplayName("seedUser - 지점장 사용자 생성")
    inner class AdminUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 00000003 없음 -> 지점장 사용자 필드 검증")
        fun createsAdminUser_whenNotExists() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val admin = employees.find { it.employeeCode == "99990003" }!!
            assertThat(admin.name).isEqualTo("지점장테스트")
            assertThat(admin.status).isEqualTo("재직")
            assertThat(admin.appLoginActive).isTrue()
            assertThat(admin.orgName).isEqualTo("테스트지점")
            assertThat(admin.role).isEqualTo(UserRole.BRANCH_MANAGER)
            assertThat(admin.password).isEqualTo("encoded_password")
            assertThat(admin.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 지점장 사용자 -> UserRole.BRANCH_MANAGER")
        fun adminUser_hasAdminRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val admin = employees.find { it.employeeCode == "99990003" }!!
            assertThat(admin.role).isEqualTo(UserRole.BRANCH_MANAGER)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000003 존재 -> 해당 사용자 save 미호출")
        fun skipsAdminUser_whenAlreadyExists() {
            // Given
            whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(true)
            whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            stubEmployeeInfoExists()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val captor = argumentCaptor<Employee>()
            verify(employeeRepository, times(5)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("99990003")
        }
    }

    @Nested
    @DisplayName("seedUser - 시스템관리자 사용자 생성 (Spec #579)")
    inner class SystemAdminUserTests {

        @Test
        @DisplayName("정상 생성 - DB에 99990099 없음 -> 시스템관리자 사용자 필드 검증")
        fun createsSystemAdminUser_whenNotExists() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val sysAdmin = employees.find { it.employeeCode == "99990099" }!!
            assertThat(sysAdmin.name).isEqualTo("시스템관리자테스트")
            assertThat(sysAdmin.role).isEqualTo(UserRole.SYSTEM_ADMIN)
            assertThat(sysAdmin.orgName).isEqualTo("본사 IT팀")
            assertThat(sysAdmin.costCenterCode).isEqualTo("9000")
            assertThat(sysAdmin.appLoginActive).isTrue()
            assertThat(sysAdmin.password).isEqualTo("encoded_password")
            assertThat(sysAdmin.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("MANAGE_PERMISSIONS 권한 충족 - SYSTEM_ADMIN 시드는 관리자 등록 API 호출 자격을 가진다")
        fun systemAdmin_satisfiesManagePermissions() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val sysAdmin = employees.find { it.employeeCode == "99990099" }!!
            assertThat(sysAdmin.role).isIn(UserRole.MANAGE_PERMISSIONS)
        }

        @Test
        @DisplayName("멱등성 - DB에 99990099 존재 -> 해당 사용자 save 미호출")
        fun skipsSystemAdmin_whenAlreadyExists() {
            // Given
            whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(true)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            stubEmployeeInfoExists()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val captor = argumentCaptor<Employee>()
            verify(employeeRepository, times(5)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeCode }
            assertThat(savedIds).doesNotContain("99990099")
        }
    }

    @Nested
    @DisplayName("seedUser - 부분 존재 및 동일 지점 검증")
    inner class PartialAndGroupTests {

        @Test
        @DisplayName("부분 존재 - 00000001만 존재 -> 나머지 5명만 생성")
        fun createsOnlyMissing_whenPartiallyExists() {
            // Given
            whenever(employeeRepository.existsByEmployeeCode("99990001")).thenReturn(true)
            whenever(employeeRepository.existsByEmployeeCode("99990002")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990003")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990004")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990005")).thenReturn(false)
            whenever(employeeRepository.existsByEmployeeCode("99990099")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }
            stubEmployeeInfoExists()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val captor = argumentCaptor<Employee>()
            verify(employeeRepository, times(5)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeCode }
            assertThat(savedIds).containsExactly("99990002", "99990003", "99990004", "99990005", "99990099")
        }

        @Test
        @DisplayName("테스트지점 소속 검증 - 00000001~00000004 테스트지점, 99990099 본사 IT팀")
        fun testBranchUsers() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val employees = captureAllSavedEmployees()
            val testBranchEmployees = employees.filter { it.orgName == "테스트지점" }
            assertThat(testBranchEmployees).hasSize(4)
            assertThat(testBranchEmployees.map { it.employeeCode })
                .containsExactlyInAnyOrder("99990001", "99990002", "99990003", "99990004")
            val headOfficeEmployees = employees.filter { it.orgName == "본사 IT팀" }
            assertThat(headOfficeEmployees).hasSize(1)
            assertThat(headOfficeEmployees[0].employeeCode).isEqualTo("99990099")
        }

        @Test
        @DisplayName("전체 멱등성 - 여섯 사용자 모두 존재 -> save 미호출")
        fun noSave_whenAllExist() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(employeeRepository, never()).save(any<Employee>())
        }
    }

    @Nested
    @DisplayName("run - GPS 동의 약관 시드 생성")
    inner class AgreementWordSeedTests {

        @Test
        @DisplayName("정상 생성 - 활성 약관 없음 -> AgreementWord 생성 및 저장")
        fun run_createsAgreementWord_whenNotExists() {
            // Given
            stubAllUsersExist()
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())
            whenever(agreementWordRepository.save(any<AgreementWord>()))
                .thenAnswer { it.getArgument<AgreementWord>(0) }
            whenever(organizationRepository.count()).thenReturn(1L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(agreementWordRepository).save(any<AgreementWord>())
        }

        @Test
        @DisplayName("멱등성 - 활성 약관 이미 존재 -> 저장 skip")
        fun run_skipsAgreementWord_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(agreementWordRepository, never()).save(any<AgreementWord>())
        }

        @Test
        @DisplayName("정상 생성 - 생성된 AgreementWord의 필드 확인")
        fun run_createsAgreementWordWithCorrectData() {
            // Given
            stubAllUsersExist()
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())
            whenever(agreementWordRepository.save(any<AgreementWord>()))
                .thenAnswer { it.getArgument<AgreementWord>(0) }
            whenever(organizationRepository.count()).thenReturn(1L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(agreementWordRepository).save(check<AgreementWord> { aw ->
                assertThat(aw.name).isEqualTo("AGR-LOCAL-001")
                assertThat(aw.contents).contains("[LOCAL 개발용]")
                assertThat(aw.contents).contains("위치정보 수집·이용 동의서")
                assertThat(aw.active).isTrue()
                assertThat(aw.isDeleted).isFalse()
                assertThat(aw.activeDate).isNotNull()
                assertThat(aw.createdAt).isNotNull()
            })
        }
    }

    @Nested
    @DisplayName("seedAccount - 거래처 시드 생성")
    inner class AccountSeedTests {

        @Test
        @DisplayName("정상 생성 - 거래처 없음 -> 8건 생성")
        fun createsAccounts_whenNotExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            stubAllAccountsNotExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(accountRepository, times(8)).save(any<Account>())
        }

        @Test
        @DisplayName("멱등성 - 거래처 이미 존재 -> save 미호출")
        fun skipsAccounts_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(accountRepository, never()).save(any<Account>())
        }

        @Test
        @DisplayName("테스트지점 거래처 5건 + 강남지점 거래처 3건 검증")
        fun createsAccountsWithCorrectBranches() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            stubAllAccountsNotExist()

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            val captor = argumentCaptor<Account>()
            verify(accountRepository, times(8)).save(captor.capture())

            val testBranch = captor.allValues.filter { it.branchCode == "1111" }
            val gangnamBranch = captor.allValues.filter { it.branchCode == "1112" }
            assertThat(testBranch).hasSize(5)
            assertThat(gangnamBranch).hasSize(3)
        }
    }

    @Nested
    @DisplayName("run - 조직마스터 시드 생성")
    inner class OrgSeedTests {

        @Test
        @DisplayName("정상 생성 - org 테이블 비어있음 -> Org 3건 생성")
        fun run_createsOrgs_whenNotExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(organizationRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(organizationRepository).saveAll(check<List<Organization>> { orgs ->
                assertThat(orgs).hasSize(3)
            })
        }

        @Test
        @DisplayName("멱등성 - org 테이블에 데이터 존재 -> 저장 skip")
        fun run_skipsOrgs_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(organizationRepository.count()).thenReturn(3L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(organizationRepository, never()).saveAll(any<List<Organization>>())
        }

        @Test
        @DisplayName("테스트지점 연결 확인 - orgNameLevel5에 테스트지점 포함")
        fun run_createsOrgsWithTestBranch() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(organizationRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(organizationRepository).saveAll(check<List<Organization>> { orgs ->
                val testBranch = orgs.filter { it.orgNameLevel5 == "테스트지점" }
                assertThat(testBranch).hasSize(1)
                assertThat(testBranch[0].costCenterLevel5).isEqualTo("1111")
            })
        }

        @Test
        @DisplayName("시드 데이터 필드 검증 - 3건의 계층 구조")
        fun run_createsOrgsWithCorrectData() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(organizationRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(DefaultApplicationArguments())

            // Then
            verify(organizationRepository).saveAll(check<List<Organization>> { orgs ->
                // 공통 Level2/3
                orgs.forEach { org ->
                    assertThat(org.costCenterLevel2).isEqualTo("1000")
                    assertThat(org.orgNameLevel2).isEqualTo("오뚜기")
                    assertThat(org.costCenterLevel3).isEqualTo("1100")
                    assertThat(org.orgNameLevel3).isEqualTo("영업본부")
                }

                // Level5 이름 검증
                val level5Names = orgs.map { it.orgNameLevel5 }
                assertThat(level5Names).containsExactly("테스트지점", "강남지점", "대전지점")
            })
        }
    }

}
