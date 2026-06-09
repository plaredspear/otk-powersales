package com.otoki.powersales.common.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.AccountInvalidParameterException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("MyAccountService 테스트")
class MyAccountServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepositoryCustom = mockk()

    private val myAccountService = MyAccountService(
        employeeRepository,
        accountRepository,
        teamMemberScheduleRepository,
    )

    @Nested
    @DisplayName("getMyAccounts - 일반 사원(여사원) 거래처 조회")
    inner class EmployeeAccountsTests {

        @Test
        @DisplayName("여사원 - 팀멤버스케줄 기반 거래처 조회 (진열 union 없음)")
        fun getMyAccounts_employee_teamScheduleOnly() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2, name = "(주)대한식품", externalKey = "1025173")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1, 2)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.stores[0].accountName).isEqualTo("(유)경산식품")
            assertThat(result.stores[1].accountName).isEqualTo("(주)대한식품")
        }

        @Test
        @DisplayName("여사원 - 배정 거래처 없음 -> 빈 리스트")
        fun getMyAccounts_employee_noSchedules() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("여사원 - addressDetail 필드 포함 확인")
        fun getMyAccounts_includesAddressDetail() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "경산농협", externalKey = "1025172",
                    address1 = "전라남도 목포시 용해동 123", address2 = "1층")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

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
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 10, name = "A마트", externalKey = "2001"),
                createAccount(id = 11, name = "B식품", externalKey = "2002")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every {
                accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot("1100", listOf("1000", "1010"), true)
            } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            verify(exactly = 0) { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any()) }
        }

        @Test
        @DisplayName("조장 - costCenterCode null -> 빈 리스트")
        fun getMyAccounts_leader_noCostCenterCode() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("조장 - 레거시 yang 예외 sfid -> 팀장 스케줄 기반 거래처 조회")
        fun getMyAccounts_leader_legacyScheduleException() {
            val userId = 1L
            val employee = createEmployee(
                id = userId, employeeCode = "20030117",
                role = AppAuthority.LEADER, costCenterCode = "1100",
                sfid = "a0c1y0000005452AAA"
            )
            val accounts = listOf(createAccount(id = 5, name = "사과마을", externalKey = "1014841"))

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByTeamLeaderIdAndDateRange(userId, any(), any()) } returns listOf(5)
            every { accountRepository.findByIdInAndIsDeletedNot(listOf(5), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("사과마을")
            // yang 예외는 지점코드 기반(teamleaderAccList)을 타지 않는다
            verify(exactly = 0) { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 부서장(AccountViewAll) 거래처 조회")
    inner class AccountViewAllTests {

        @Test
        @DisplayName("부서장 + scope=SALES -> 일정 잡힌 거래처 조회 (keyword/limit DB 푸시다운)")
        fun getMyAccounts_accountViewAll_salesScope_allAccounts() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.ACCOUNT_VIEW_ALL)
            val accounts = listOf(
                createAccount(id = 1, name = "A마트", externalKey = "1001"),
                createAccount(id = 2, name = "B식품", externalKey = "1002")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctScheduledAccounts(null, any()) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null, MyAccountScope.SALES)

            assertThat(result.stores).hasSize(2)
            verify { teamMemberScheduleRepository.findDistinctScheduledAccounts(null, any()) }
            verify(exactly = 0) { accountRepository.findByIdInAndIsDeletedNot(any(), any()) }
        }

        @Test
        @DisplayName("부서장 + scope=FIELD -> 전체조회 분기 없이 본인 스케줄 경로")
        fun getMyAccounts_accountViewAll_fieldScope_employeePath() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.ACCOUNT_VIEW_ALL)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()

            val result = myAccountService.getMyAccounts(userId, null, MyAccountScope.FIELD)

            assertThat(result.stores).isEmpty()
            verify(exactly = 0) { teamMemberScheduleRepository.findDistinctScheduledAccounts(any(), any()) }
            verify { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) }
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 키워드 검색")
    inner class KeywordSearchTests {

        @Test
        @DisplayName("키워드 검색 (거래처명) - '경산' 검색 -> 매칭 결과만 반환")
        fun getMyAccounts_searchByName() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2, name = "(주)대한식품", externalKey = "1025173")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, "경산")

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("(유)경산식품")
        }

        @Test
        @DisplayName("키워드 검색 (SAP 코드) - '1025' 검색 -> 매칭 결과만 반환")
        fun getMyAccounts_searchByCode() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "A마트", externalKey = "1025172"),
                createAccount(id = 2, name = "B식품", externalKey = "2001")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, "1025")

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountCode).isEqualTo("1025172")
        }

        @Test
        @DisplayName("키워드 1자 - AccountInvalidParameterException 예외")
        fun getMyAccounts_keywordTooShort() {
            assertThatThrownBy { myAccountService.getMyAccounts(1L, "가") }
                .isInstanceOf(AccountInvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 에러 처리")
    inner class ErrorTests {

        @Test
        @DisplayName("사용자 미존재 - EmployeeNotFoundException 예외")
        fun getMyAccounts_userNotFound() {
            every { employeeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { myAccountService.getMyAccounts(999L, null) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 정렬")
    inner class SortTests {

        @Test
        @DisplayName("거래처명 오름차순 정렬")
        fun getMyAccounts_sortedByAccountName() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 1, name = "홈플러스 서면점", externalKey = "1025173"),
                createAccount(id = 2, name = "가나다식품", externalKey = "1025172"),
                createAccount(id = 3, name = "나라마트", externalKey = "1025174")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(any(), any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(3)
            assertThat(result.stores[0].accountName).isEqualTo("가나다식품")
            assertThat(result.stores[1].accountName).isEqualTo("나라마트")
            assertThat(result.stores[2].accountName).isEqualTo("홈플러스 서면점")
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        sfid: String? = null,
        role: String? = null,
        costCenterCode: String? = null
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            password = "encodedPassword",
            name = "테스트 사용자",
            orgName = "부산지점",
            passwordChangeRequired = false,
            sfid = sfid,
            role = role,
            costCenterCode = costCenterCode
        )
    }

    private fun createAccount(
        id: Long = 1L,
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
