package com.otoki.powersales.platform.common.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.exception.AccountInvalidParameterException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepositoryCustom
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepositoryCustom
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepositoryCustom = mockk()

    private val myAccountService = MyAccountService(
        employeeRepository,
        accountRepository,
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
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

            assertThat(result.accounts).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.accounts[0].accountName).isEqualTo("(유)경산식품")
            assertThat(result.accounts[1].accountName).isEqualTo("(주)대한식품")
        }

        @Test
        @DisplayName("여사원 - 배정 거래처 없음 -> 빈 리스트")
        fun getMyAccounts_employee_noSchedules() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.accounts).isEmpty()
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

            assertThat(result.accounts[0].address).isEqualTo("전라남도 목포시 용해동 123")
            assertThat(result.accounts[0].addressDetail).isEqualTo("1층")
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

            assertThat(result.accounts).hasSize(2)
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

            assertThat(result.accounts).isEmpty()
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

            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountName).isEqualTo("사과마을")
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

            assertThat(result.accounts).hasSize(2)
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

            assertThat(result.accounts).isEmpty()
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

            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountName).isEqualTo("(유)경산식품")
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

            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountCode).isEqualTo("1025172")
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

            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts[0].accountName).isEqualTo("가나다식품")
            assertThat(result.accounts[1].accountName).isEqualTo("나라마트")
            assertThat(result.accounts[2].accountName).isEqualTo("홈플러스 서면점")
        }
    }

    @Nested
    @DisplayName("getMyAccounts - 주문(ORDER) 거래처 조회")
    inner class OrderScopeTests {

        @Test
        @DisplayName("여사원 + ORDER -> 팀멤버스케줄 ∪ 진열 일정, 주문가능 abctype만 반환")
        fun getMyAccounts_order_unionDisplayAndAbcFilter() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            // TMS [1,2] ∪ Display [2,3] = [1,2,3] (2 중복 제거)
            val accounts = listOf(
                createAccount(id = 1, name = "주문가능마트", externalKey = "1001", abcTypeCode = "2001"),
                createAccount(id = 2, name = "주문불가식품", externalKey = "1002", abcTypeCode = "9999"),
                createAccount(id = 3, name = "진열주문마트", externalKey = "1003", abcTypeCode = "5104")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1, 2)
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(2, 3)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null, MyAccountScope.ORDER)

            // abctype 미해당(9999) 제외 -> 주문가능 2건만
            assertThat(result.accounts).hasSize(2)
            assertThat(result.accounts.map { it.accountName })
                .containsExactlyInAnyOrder("주문가능마트", "진열주문마트")
            verify { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) }
        }

        @Test
        @DisplayName("여사원 + FIELD -> 진열 union 미적용 (진열 레포 미호출)")
        fun getMyAccounts_field_noDisplayUnion() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(createAccount(id = 1, name = "현장마트", externalKey = "1001", abcTypeCode = "9999"))

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null, MyAccountScope.FIELD)

            // FIELD 는 abctype 필터 없이 그대로 반환
            assertThat(result.accounts).hasSize(1)
            verify(exactly = 0) { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any()) }
        }

        @Test
        @DisplayName("일반 조장 + ORDER -> teamleaderAccList 그대로 (abctype 미적용, 진열 union 없음)")
        fun getMyAccounts_order_leaderUnchanged() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = AppAuthority.LEADER, costCenterCode = "1100")
            val accounts = listOf(
                createAccount(id = 10, name = "A마트", externalKey = "2001", abcTypeCode = "9999"),
                createAccount(id = 11, name = "B식품", externalKey = "2002", abcTypeCode = "0000")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every {
                accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot("1100", listOf("1000", "1010"), true)
            } returns accounts

            val result = myAccountService.getMyAccounts(userId, null, MyAccountScope.ORDER)

            // 레거시 teamleaderAccList 는 abctype 주석 처리 -> 2건 모두 반환
            assertThat(result.accounts).hasSize(2)
            verify(exactly = 0) { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any()) }
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
        phone: String? = "061-123-4567",
        abcTypeCode: String? = null
    ): Account {
        return Account(
            id = id,
            externalKey = externalKey,
            name = name,
            address1 = address1,
            address2 = address2,
            representative = representative,
            phone = phone,
            abcTypeCode = abcTypeCode
        )
    }
}
