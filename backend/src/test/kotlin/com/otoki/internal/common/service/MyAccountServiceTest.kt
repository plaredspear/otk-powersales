package com.otoki.internal.common.service

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.User
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.common.exception.AccountInvalidParameterException
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.schedule.repository.DisplayWorkScheduleRepositoryCustom
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.internal.sap.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("MyAccountService 테스트")
class MyAccountServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepositoryCustom

    @Mock
    private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepositoryCustom

    @InjectMocks
    private lateinit var myAccountService: MyAccountService

    @Nested
    @DisplayName("getMyAccounts - 일반 사원 거래처 조회")
    inner class EmployeeAccountsTests {

        @Test
        @DisplayName("일반 사원 - 팀멤버+진열스케줄 기반 거래처 조회, 중복 제거")
        fun getMyAccounts_employee_mergedSchedules() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2, name = "(주)대한식품", externalKey = "1025173"),
                createAccount(id = 3, name = "나라마트", externalKey = "1025174")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(listOf(1, 2))
            whenever(displayWorkScheduleRepository.findDistinctAccountIdsBySfidAndDateRange(eq("SF001"), any(), any()))
                .thenReturn(listOf(2, 3))
            whenever(accountRepository.findByIdInAndIsDeletedNot(any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            // 거래처명 오름차순 정렬
            assertThat(result.stores[0].accountName).isEqualTo("(유)경산식품")
            assertThat(result.stores[1].accountName).isEqualTo("(주)대한식품")
            assertThat(result.stores[2].accountName).isEqualTo("나라마트")
        }

        @Test
        @DisplayName("일반 사원 - 팀멤버스케줄만 있는 거래처 포함")
        fun getMyAccounts_employee_teamScheduleOnly() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = "SF001")
            val accounts = listOf(createAccount(id = 1, name = "경산농협", externalKey = "1025172"))

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(listOf(1))
            whenever(displayWorkScheduleRepository.findDistinctAccountIdsBySfidAndDateRange(eq("SF001"), any(), any()))
                .thenReturn(emptyList())
            whenever(accountRepository.findByIdInAndIsDeletedNot(any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("경산농협")
        }

        @Test
        @DisplayName("일반 사원 - 진열스케줄만 있는 거래처 포함")
        fun getMyAccounts_employee_displayScheduleOnly() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = "SF001")
            val accounts = listOf(createAccount(id = 3, name = "나라마트", externalKey = "1025174"))

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(emptyList())
            whenever(displayWorkScheduleRepository.findDistinctAccountIdsBySfidAndDateRange(eq("SF001"), any(), any()))
                .thenReturn(listOf(3))
            whenever(accountRepository.findByIdInAndIsDeletedNot(any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("나라마트")
        }

        @Test
        @DisplayName("일반 사원 - 배정 거래처 없음 -> 빈 리스트")
        fun getMyAccounts_employee_noSchedules() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = "SF001")

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(emptyList())
            whenever(displayWorkScheduleRepository.findDistinctAccountIdsBySfidAndDateRange(eq("SF001"), any(), any()))
                .thenReturn(emptyList())

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일반 사원 - sfid null이면 진열스케줄 조회 건너뜀")
        fun getMyAccounts_employee_nullSfid() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = null)
            val accounts = listOf(createAccount(id = 1, name = "경산농협", externalKey = "1025172"))

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(listOf(1))
            whenever(accountRepository.findByIdInAndIsDeletedNot(any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(1)
            verify(displayWorkScheduleRepository, never()).findDistinctAccountIdsBySfidAndDateRange(any(), any(), any())
        }

        @Test
        @DisplayName("addressDetail 필드 포함 확인")
        fun getMyAccounts_includesAddressDetail() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "경산농협", externalKey = "1025172",
                    address1 = "전라남도 목포시 용해동 123", address2 = "1층")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeNumberAndDateRange(eq("20030117"), any(), any()))
                .thenReturn(listOf(1))
            whenever(displayWorkScheduleRepository.findDistinctAccountIdsBySfidAndDateRange(eq("SF001"), any(), any()))
                .thenReturn(emptyList())
            whenever(accountRepository.findByIdInAndIsDeletedNot(any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores[0].address).isEqualTo("전라남도 목포시 용해동 123")
            assertThat(result.stores[0].addressDetail).isEqualTo("1층")
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 조장 거래처 조회")
    inner class LeaderAccountsTests {

        @Test
        @DisplayName("조장 - 지점코드 기반 거래처 조회")
        fun getMyAccounts_leader_branchAccounts() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", appAuthority = "조장", costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 10, name = "A마트", externalKey = "2001"),
                createAccount(id = 11, name = "B식품", externalKey = "2002")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                eq("1100"), eq(listOf("1000", "1010")), eq(true)
            )).thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            // 조장은 팀멤버/진열스케줄 조회하지 않음
            verify(teamMemberScheduleRepository, never()).findDistinctAccountIdsByEmployeeNumberAndDateRange(any(), any(), any())
            verify(displayWorkScheduleRepository, never()).findDistinctAccountIdsBySfidAndDateRange(any(), any(), any())
        }

        @Test
        @DisplayName("조장 - costCenterCode null -> 빈 리스트")
        fun getMyAccounts_leader_noCostCenterCode() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", appAuthority = "조장", costCenterCode = null)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 키워드 검색")
    inner class KeywordSearchTests {

        @Test
        @DisplayName("키워드 검색 (거래처명) - '경산' 검색 -> 매칭 결과만 반환")
        fun getMyAccounts_searchByName() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", appAuthority = "조장", costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2, name = "(주)대한식품", externalKey = "1025173")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, "경산")

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("키워드 검색 (SAP 코드) - '1025' 검색 -> 매칭 결과만 반환")
        fun getMyAccounts_searchByCode() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", appAuthority = "조장", costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "A마트", externalKey = "1025172"),
                createAccount(id = 2, name = "B식품", externalKey = "2001")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, "1025")

            // Then
            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountCode).isEqualTo("1025172")
        }

        @Test
        @DisplayName("키워드 1자 - AccountInvalidParameterException 예외")
        fun getMyAccounts_keywordTooShort() {
            // When & Then
            assertThatThrownBy { myAccountService.getMyAccounts(1L, "가") }
                .isInstanceOf(AccountInvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 에러 처리")
    inner class ErrorTests {

        @Test
        @DisplayName("사용자 미존재 - UserNotFoundException 예외")
        fun getMyAccounts_userNotFound() {
            // Given
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { myAccountService.getMyAccounts(999L, null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 정렬")
    inner class SortTests {

        @Test
        @DisplayName("거래처명 오름차순 정렬")
        fun getMyAccounts_sortedByAccountName() {
            // Given
            val userId = 1L
            val user = createUser(id = userId, employeeNumber = "20030117", appAuthority = "조장", costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "홈플러스 서면점", externalKey = "1025173"),
                createAccount(id = 2, name = "가나다식품", externalKey = "1025172"),
                createAccount(id = 3, name = "나라마트", externalKey = "1025174")
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), eq(true)))
                .thenReturn(accounts)

            // When
            val result = myAccountService.getMyAccounts(userId, null)

            // Then
            assertThat(result.stores).hasSize(3)
            assertThat(result.stores[0].accountName).isEqualTo("가나다식품")
            assertThat(result.stores[1].accountName).isEqualTo("나라마트")
            assertThat(result.stores[2].accountName).isEqualTo("홈플러스 서면점")
        }
    }

    // ========== Helpers ==========

    private fun createUser(
        id: Long = 1L,
        employeeNumber: String = "12345678",
        sfid: String? = null,
        appAuthority: String? = null,
        costCenterCode: String? = null
    ): User {
        return User(
            id = id,
            employeeNumber = employeeNumber,
            password = "encodedPassword",
            name = "테스트 사용자",
            orgName = "부산지점",
            passwordChangeRequired = false,
            sfid = sfid,
            appAuthority = appAuthority,
            costCenterCode = costCenterCode
        )
    }

    private fun createAccount(
        id: Int = 1,
        externalKey: String = "1025172",
        name: String = "(유)경산식품",
        address1: String? = "전라남도 목포시",
        address2: String? = null,
        representative: String? = "김정자",
        phone: String? = "061-123-4567"
    ): Account {
        return Account(
            id = id,
            externalKey = externalKey,
            name = name,
            address1 = address1,
            address2 = address2,
            representative = representative,
            phone = phone
        )
    }
}
