package com.otoki.powersales.common.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.AccountInvalidParameterException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepositoryCustom
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
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepositoryCustom = mockk()

    private val myAccountService = MyAccountService(
        employeeRepository,
        accountRepository,
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
    )

    @Nested
    @DisplayName("getMyAccounts - 일반 사원 거래처 조회")
    inner class EmployeeAccountsTests {

        @Test
        @DisplayName("일반 사원 - 팀멤버+진열스케줄 기반 거래처 조회, 중복 제거")
        fun getMyAccounts_employee_mergedSchedules() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "(유)경산식품", externalKey = "1025172"),
                createAccount(id = 2, name = "(주)대한식품", externalKey = "1025173"),
                createAccount(id = 3, name = "나라마트", externalKey = "1025174")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1, 2)
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(2, 3)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(3)
            assertThat(result.totalCount).isEqualTo(3)
            assertThat(result.stores[0].accountName).isEqualTo("(유)경산식품")
            assertThat(result.stores[1].accountName).isEqualTo("(주)대한식품")
            assertThat(result.stores[2].accountName).isEqualTo("나라마트")
        }

        @Test
        @DisplayName("일반 사원 - 팀멤버스케줄만 있는 거래처 포함")
        fun getMyAccounts_employee_teamScheduleOnly() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(createAccount(id = 1, name = "경산농협", externalKey = "1025172"))

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1)
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("경산농협")
        }

        @Test
        @DisplayName("일반 사원 - 진열스케줄만 있는 거래처 포함")
        fun getMyAccounts_employee_displayScheduleOnly() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(createAccount(id = 3, name = "나라마트", externalKey = "1025174"))

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(3)
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(1)
            assertThat(result.stores[0].accountName).isEqualTo("나라마트")
        }

        @Test
        @DisplayName("일반 사원 - 배정 거래처 없음 -> 빈 리스트")
        fun getMyAccounts_employee_noSchedules() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일반 사원 - 진열스케줄도 userId 기반으로 조회")
        fun getMyAccounts_employee_displayScheduleByUserId() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = null)
            val accounts = listOf(createAccount(id = 1, name = "경산농협", externalKey = "1025172"))

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1)
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()
            every { accountRepository.findByIdInAndIsDeletedNot(any(), true) } returns accounts

            val result = myAccountService.getMyAccounts(userId, null)

            assertThat(result.stores).hasSize(1)
            verify { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) }
        }

        @Test
        @DisplayName("addressDetail 필드 포함 확인")
        fun getMyAccounts_includesAddressDetail() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", sfid = "SF001")
            val accounts = listOf(
                createAccount(id = 1, name = "경산농협", externalKey = "1025172",
                    address1 = "전라남도 목포시 용해동 123", address2 = "1층")
            )

            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { teamMemberScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns listOf(1)
            every { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(userId, any(), any()) } returns emptyList()
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
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = UserRoleEnum.LEADER, costCenterCode = "1100")
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
            verify(exactly = 0) { displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndDateRange(any(), any(), any()) }
        }

        @Test
        @DisplayName("조장 - costCenterCode null -> 빈 리스트")
        fun getMyAccounts_leader_noCostCenterCode() {
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = UserRoleEnum.LEADER, costCenterCode = null)

            every { employeeRepository.findById(userId) } returns Optional.of(employee)

            val result = myAccountService.getMyAccounts(userId, null)

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
            val userId = 1L
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = UserRoleEnum.LEADER, costCenterCode = "1100")
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
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = UserRoleEnum.LEADER, costCenterCode = "1100")
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
        @DisplayName("사용자 미존재 - UserNotFoundException 예외")
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
            val employee = createEmployee(id = userId, employeeCode = "20030117", role = UserRoleEnum.LEADER, costCenterCode = "1100")
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
        role: UserRoleEnum? = null,
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
