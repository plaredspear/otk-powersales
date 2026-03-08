package com.otoki.internal.common.config

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("LocalDataInitializer 테스트")
class LocalDataInitializerTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var agreementWordRepository: AgreementWordRepository

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @Mock
    private lateinit var orgRepository: OrgRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @InjectMocks
    private lateinit var localDataInitializer: LocalDataInitializer

    private fun stubAllUsersNotExist() {
        whenever(userRepository.existsByEmployeeId("00000001")).thenReturn(false)
        whenever(userRepository.existsByEmployeeId("00000002")).thenReturn(false)
        whenever(userRepository.existsByEmployeeId("00000003")).thenReturn(false)
        whenever(userRepository.existsByEmployeeId("00000004")).thenReturn(false)
        whenever(userRepository.existsByEmployeeId("00000005")).thenReturn(false)
        whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
        whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
    }

    private fun stubAllUsersExist() {
        whenever(userRepository.existsByEmployeeId("00000001")).thenReturn(true)
        whenever(userRepository.existsByEmployeeId("00000002")).thenReturn(true)
        whenever(userRepository.existsByEmployeeId("00000003")).thenReturn(true)
        whenever(userRepository.existsByEmployeeId("00000004")).thenReturn(true)
        whenever(userRepository.existsByEmployeeId("00000005")).thenReturn(true)
    }

    private fun stubOtherSeedsExist() {
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.of(AgreementWord()))
        whenever(orgRepository.count()).thenReturn(1L)
        whenever(accountRepository.count()).thenReturn(1L)
    }

    private fun captureAllSavedUsers(): List<User> {
        val captor = argumentCaptor<User>()
        verify(userRepository, times(5)).save(captor.capture())
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
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val leader = users.find { it.employeeId == "00000001" }!!
            assertThat(leader.name).isEqualTo("개발테스트")
            assertThat(leader.status).isEqualTo("재직")
            assertThat(leader.appLoginActive).isTrue()
            assertThat(leader.orgName).isEqualTo("테스트지점")
            assertThat(leader.appAuthority).isEqualTo("영업지원실")
            assertThat(leader.password).isEqualTo("encoded_password")
            assertThat(leader.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 영업지원실 사용자 -> UserRole.USER")
        fun leaderUser_hasUserRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val leader = users.find { it.employeeId == "00000001" }!!
            assertThat(leader.role).isEqualTo(UserRole.USER)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000001 존재 -> save 미호출")
        fun skipsLeader_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository, never()).save(any<User>())
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
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val salesUser = users.find { it.employeeId == "00000002" }!!
            assertThat(salesUser.name).isEqualTo("여사원테스트")
            assertThat(salesUser.status).isEqualTo("재직")
            assertThat(salesUser.appLoginActive).isTrue()
            assertThat(salesUser.orgName).isEqualTo("테스트지점")
            assertThat(salesUser.appAuthority).isEqualTo("여사원")
            assertThat(salesUser.password).isEqualTo("encoded_password")
            assertThat(salesUser.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 여사원 사용자 -> UserRole.USER")
        fun salesUser_hasUserRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val salesUser = users.find { it.employeeId == "00000002" }!!
            assertThat(salesUser.role).isEqualTo(UserRole.USER)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000002 존재 -> 해당 사용자 save 미호출")
        fun skipsSalesUser_whenAlreadyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000001")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000002")).thenReturn(true)
            whenever(userRepository.existsByEmployeeId("00000003")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000004")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000005")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val captor = argumentCaptor<User>()
            verify(userRepository, times(4)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeId }
            assertThat(savedIds).doesNotContain("00000002")
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
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val admin = users.find { it.employeeId == "00000003" }!!
            assertThat(admin.name).isEqualTo("지점장테스트")
            assertThat(admin.status).isEqualTo("재직")
            assertThat(admin.appLoginActive).isTrue()
            assertThat(admin.orgName).isEqualTo("테스트지점")
            assertThat(admin.appAuthority).isEqualTo("지점장")
            assertThat(admin.password).isEqualTo("encoded_password")
            assertThat(admin.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("역할 검증 - 지점장 사용자 -> UserRole.ADMIN")
        fun adminUser_hasAdminRole() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val admin = users.find { it.employeeId == "00000003" }!!
            assertThat(admin.role).isEqualTo(UserRole.ADMIN)
        }

        @Test
        @DisplayName("멱등성 - DB에 00000003 존재 -> 해당 사용자 save 미호출")
        fun skipsAdminUser_whenAlreadyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000001")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000002")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000003")).thenReturn(true)
            whenever(userRepository.existsByEmployeeId("00000004")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000005")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val captor = argumentCaptor<User>()
            verify(userRepository, times(4)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeId }
            assertThat(savedIds).doesNotContain("00000003")
        }
    }

    @Nested
    @DisplayName("seedUser - 부분 존재 및 동일 지점 검증")
    inner class PartialAndGroupTests {

        @Test
        @DisplayName("부분 존재 - 00000001만 존재 -> 나머지 4명만 생성")
        fun createsOnlyMissing_whenPartiallyExists() {
            // Given
            whenever(userRepository.existsByEmployeeId("00000001")).thenReturn(true)
            whenever(userRepository.existsByEmployeeId("00000002")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000003")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000004")).thenReturn(false)
            whenever(userRepository.existsByEmployeeId("00000005")).thenReturn(false)
            whenever(passwordEncoder.encode("1234")).thenReturn("encoded_password")
            whenever(userRepository.save(any<User>())).thenAnswer { it.getArgument<User>(0) }
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val captor = argumentCaptor<User>()
            verify(userRepository, times(4)).save(captor.capture())
            val savedIds = captor.allValues.map { it.employeeId }
            assertThat(savedIds).containsExactly("00000002", "00000003", "00000004", "00000005")
        }

        @Test
        @DisplayName("테스트지점 소속 검증 - 00000001~00000003 테스트지점")
        fun testBranchUsers() {
            // Given
            stubAllUsersNotExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            val users = captureAllSavedUsers()
            val testBranchUsers = users.filter { it.orgName == "테스트지점" }
            assertThat(testBranchUsers).hasSize(3)
            assertThat(testBranchUsers.map { it.employeeId })
                .containsExactlyInAnyOrder("00000001", "00000002", "00000003")
        }

        @Test
        @DisplayName("전체 멱등성 - 다섯 사용자 모두 존재 -> save 미호출")
        fun noSave_whenAllExist() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()

            // When
            localDataInitializer.run(null)

            // Then
            verify(userRepository, never()).save(any<User>())
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
            whenever(orgRepository.count()).thenReturn(1L)
            whenever(accountRepository.count()).thenReturn(1L)

            // When
            localDataInitializer.run(null)

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
            localDataInitializer.run(null)

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
            whenever(orgRepository.count()).thenReturn(1L)
            whenever(accountRepository.count()).thenReturn(1L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(agreementWordRepository).save(check<AgreementWord> { aw ->
                assertThat(aw.name).isEqualTo("AGR-LOCAL-001")
                assertThat(aw.contents).contains("[LOCAL 개발용]")
                assertThat(aw.contents).contains("위치정보 수집·이용 동의서")
                assertThat(aw.active).isTrue()
                assertThat(aw.isDeleted).isFalse()
                assertThat(aw.activeDate).isNotNull()
                assertThat(aw.createdDate).isNotNull()
            })
        }
    }

    @Nested
    @DisplayName("run - 공지사항 시드 생성")
    inner class NoticeSeedTests {

        @Test
        @DisplayName("정상 생성 - 공지 없음 -> Notice 5건 생성")
        fun run_createsNotices_whenNotExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(noticeRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(noticeRepository).saveAll(check<List<Notice>> { notices ->
                assertThat(notices).hasSize(5)
            })
        }

        @Test
        @DisplayName("멱등성 - 공지 이미 존재 -> 저장 skip")
        fun run_skipsNotices_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(noticeRepository.count()).thenReturn(3L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(noticeRepository, never()).saveAll(any<List<Notice>>())
        }

        @Test
        @DisplayName("정상 생성 - 시드 데이터 필드 검증")
        fun run_createsNoticesWithCorrectData() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(noticeRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(noticeRepository).saveAll(check<List<Notice>> { notices ->
                val companyNotices = notices.filter { it.category == NoticeCategory.COMPANY }
                val educationNotices = notices.filter { it.category == NoticeCategory.EDUCATION }
                assertThat(companyNotices).hasSize(2)
                assertThat(educationNotices).hasSize(1)
                (companyNotices + educationNotices).forEach { notice ->
                    assertThat(notice.scope).isEqualTo("전체")
                    assertThat(notice.branch).isNull()
                    assertThat(notice.branchCode).isNull()
                    assertThat(notice.isDeleted).isFalse()
                    assertThat(notice.contents).contains("[LOCAL 개발용]")
                }

                val branchNotices = notices.filter { it.category == NoticeCategory.BRANCH }
                assertThat(branchNotices).hasSize(2)
                branchNotices.forEach { notice ->
                    assertThat(notice.scope).isEqualTo("지점")
                    assertThat(notice.branch).isEqualTo("테스트지점")
                    assertThat(notice.branchCode).isEqualTo("BR-TEST-001")
                    assertThat(notice.isDeleted).isFalse()
                }

                val eduNotice = notices.find { it.name == "NTC-LOCAL-003" }
                assertThat(eduNotice?.eduCategory).isEqualTo("교육")

                val dates = notices.mapNotNull { it.createdDate }
                assertThat(dates).hasSize(5)
                assertThat(dates).isSorted()
            })
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
            whenever(orgRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(orgRepository).saveAll(check<List<Org>> { orgs ->
                assertThat(orgs).hasSize(3)
            })
        }

        @Test
        @DisplayName("멱등성 - org 테이블에 데이터 존재 -> 저장 skip")
        fun run_skipsOrgs_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(orgRepository.count()).thenReturn(3L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(orgRepository, never()).saveAll(any<List<Org>>())
        }

        @Test
        @DisplayName("테스트지점 연결 확인 - orgNameLevel5에 테스트지점 포함")
        fun run_createsOrgsWithTestBranch() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(orgRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(orgRepository).saveAll(check<List<Org>> { orgs ->
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
            whenever(orgRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(orgRepository).saveAll(check<List<Org>> { orgs ->
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

    @Nested
    @DisplayName("run - 거래처 마스터 시드 생성")
    inner class AccountSeedTests {

        @Test
        @DisplayName("정상 생성 - account 테이블 비어있음 -> Account 5건 생성")
        fun run_createsAccounts_whenNotExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(accountRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(accountRepository).saveAll(check<List<Account>> { accounts ->
                assertThat(accounts).hasSize(5)
            })
        }

        @Test
        @DisplayName("멱등성 - account 테이블에 데이터 존재 -> 저장 skip")
        fun run_skipsAccounts_whenAlreadyExists() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(accountRepository.count()).thenReturn(5L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(accountRepository, never()).saveAll(any<List<Account>>())
        }

        @Test
        @DisplayName("시드 데이터 필드 검증 - branchCode와 Org 매핑")
        fun run_createsAccountsWithCorrectData() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(accountRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(accountRepository).saveAll(check<List<Account>> { accounts ->
                // 이름 검증
                val names = accounts.map { it.name }
                assertThat(names).containsExactly(
                    "GS25 역삼점", "이마트 강남점", "CU 서초중앙점", "홈플러스 논현점", "세븐일레븐 대전둔산점"
                )

                // branchCode 매핑 검증
                val testBranchAccounts = accounts.filter { it.branchCode == "1111" }
                assertThat(testBranchAccounts).hasSize(3)

                val gangnamAccount = accounts.find { it.branchCode == "1112" }!!
                assertThat(gangnamAccount.branchName).isEqualTo("강남지점")

                val daejeonAccount = accounts.find { it.branchCode == "1121" }!!
                assertThat(daejeonAccount.branchName).isEqualTo("대전지점")

                // employeeCode 매핑 검증
                val emp02Accounts = accounts.filter { it.employeeCode == "00000002" }
                assertThat(emp02Accounts).hasSize(2)

                val emp01Accounts = accounts.filter { it.employeeCode == "00000001" }
                assertThat(emp01Accounts).hasSize(2)

                // 거래중지 건 검증
                val stoppedAccount = accounts.find { it.accountStatusCode == "02" }!!
                assertThat(stoppedAccount.name).isEqualTo("세븐일레븐 대전둔산점")
                assertThat(stoppedAccount.distribution).isEqualTo("N")
                assertThat(stoppedAccount.accountStatusName).isEqualTo("거래중지")

                // 공통 필드 검증
                accounts.forEach { account ->
                    assertThat(account.businessType).isEqualTo("소매")
                    assertThat(account.divisionCode).isEqualTo("1000")
                    assertThat(account.divisionName).isEqualTo("식품사업부")
                    assertThat(account.consignmentAcc).isEqualTo("N")
                    assertThat(account.werk1).isEqualTo("1000")
                    assertThat(account.werk1Tx).isEqualTo("오뚜기")
                    assertThat(account.isDeleted).isFalse()
                    assertThat(account.createdDate).isNotNull()
                    assertThat(account.createdAt).isNotNull()
                    assertThat(account.updatedAt).isNotNull()
                }
            })
        }

        @Test
        @DisplayName("orgCd 매핑 검증 - branchCode별 orgCd3/4/5 일치")
        fun run_createsAccountsWithCorrectOrgCodes() {
            // Given
            stubAllUsersExist()
            stubOtherSeedsExist()
            whenever(accountRepository.count()).thenReturn(0L)

            // When
            localDataInitializer.run(null)

            // Then
            verify(accountRepository).saveAll(check<List<Account>> { accounts ->
                // 테스트지점 (branchCode=1111)
                accounts.filter { it.branchCode == "1111" }.forEach { account ->
                    assertThat(account.orgCd3).isEqualTo("O110")
                    assertThat(account.orgCd4).isEqualTo("O111")
                    assertThat(account.orgCd5).isEqualTo("O1111")
                }

                // 강남지점 (branchCode=1112)
                val gangnam = accounts.find { it.branchCode == "1112" }!!
                assertThat(gangnam.orgCd5).isEqualTo("O1112")

                // 대전지점 (branchCode=1121)
                val daejeon = accounts.find { it.branchCode == "1121" }!!
                assertThat(daejeon.orgCd4).isEqualTo("O112")
                assertThat(daejeon.orgCd5).isEqualTo("O1121")
            })
        }
    }
}
