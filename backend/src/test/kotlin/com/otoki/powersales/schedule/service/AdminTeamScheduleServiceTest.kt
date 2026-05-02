package com.otoki.powersales.schedule.service

import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminTeamScheduleService 테스트")
class AdminTeamScheduleServiceTest {

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var adminEmployeeHolder: AdminEmployeeHolder

    @Mock
    private lateinit var adminMonthlyIntegrationService: AdminMonthlyIntegrationService

    private lateinit var service: AdminTeamScheduleService

    @BeforeEach
    fun setUpService() {
        val teamScheduleValidator = TeamScheduleValidator(
            teamMemberScheduleRepository,
            displayWorkScheduleRepository
        )
        service = AdminTeamScheduleService(
            teamMemberScheduleRepository = teamMemberScheduleRepository,
            employeeRepository = employeeRepository,
            accountRepository = accountRepository,
            adminEmployeeHolder = adminEmployeeHolder,
            adminMonthlyIntegrationService = adminMonthlyIntegrationService,
            teamScheduleValidator = teamScheduleValidator
        )
    }

    // --- Helper factories ---

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "USR_SFID_001",
        employeeCode: String = "20030001",
        name: String = "테스트사원",
        status: String? = "재직",
        role: UserRole? = null,
        costCenterCode: String? = "1234",
        isDeleted: Boolean? = false
    ): Employee = Employee(
        id = id,
        sfid = sfid,
        employeeCode = employeeCode,
        name = name,
        status = status,
        role = role,
        costCenterCode = costCenterCode,
        isDeleted = isDeleted
    )

    private fun createAccount(
        id: Int = 1,
        sfid: String? = "ACC_SFID_001",
        name: String = "테스트거래처",
        externalKey: String? = "ACC001",
        accountGroup: String? = "1010",
        branchCode: String? = "1234",
        isDeleted: Boolean? = false
    ): Account = Account(
        id = id,
        sfid = sfid,
        name = name,
        externalKey = externalKey,
        accountGroup = accountGroup,
        branchCode = branchCode,
        isDeleted = isDeleted
    )

    private fun createSchedule(
        id: Long = 1L,
        employeeId: Long? = 1L,
        employeeCode: String? = null,
        employeeName: String? = null,
        workingDate: LocalDate? = LocalDate.of(2026, 4, 1),
        workingType: String? = "근무",
        workingCategory1: String? = "진열",
        workingCategory2: String? = null,
        workingCategory3: String? = "고정",
        accountId: Int? = 1,
        accountName: String? = null,
        accountExternalKey: String? = null,
        teamLeaderId: Long? = 99L,
        commuteLogId: String? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = employeeId?.let {
            Employee(
                id = it,
                employeeCode = employeeCode ?: "EMP$it",
                name = employeeName ?: "테스트$it"
            )
        },
        workingDate = workingDate,
        workingType = workingType,
        workingCategory1 = workingCategory1,
        workingCategory2 = workingCategory2,
        workingCategory3 = workingCategory3,
        account = accountId?.let {
            Account(
                id = it,
                name = accountName ?: "거래처$it",
                externalKey = accountExternalKey
            )
        },
        teamLeader = teamLeaderId?.let { Employee(id = it, employeeCode = "EMP$it", name = "팀장$it") },
        commuteLogId = commuteLogId
    )

    // ========== getMembers ==========

    @Nested
    @DisplayName("getMembers - 여사원 목록 조회")
    inner class GetMembersTests {

        @Test
        @DisplayName("정상 조회 - 조장의 costCenterCode에 속한 여사원 목록 반환")
        fun getMembers_success() {
            // Given
            val leader = createEmployee(id = 10L, costCenterCode = "1234", role = UserRole.LEADER)
            val member1 = createEmployee(id = 2L, sfid = "USR_002", employeeCode = "20030002", name = "김영희", role = UserRole.WOMAN)
            val member2 = createEmployee(id = 3L, sfid = "USR_003", employeeCode = "20030003", name = "이수진", role = UserRole.WOMAN)

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(employeeRepository.findWithEmployeeInfoByCostCenterCodeAndRole("1234", UserRole.WOMAN))
                .thenReturn(listOf(member1, member2))

            // When
            val result = service.getMembers(10L)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].employeeCode).isEqualTo("20030002")
            assertThat(result[0].name).isEqualTo("김영희")
            assertThat(result[1].employeeCode).isEqualTo("20030003")
            assertThat(result[1].name).isEqualTo("이수진")
        }

        @Test
        @DisplayName("빈 결과 - costCenterCode가 null인 경우 빈 목록 반환")
        fun getMembers_costCenterCodeNull_returnsEmpty() {
            // Given
            val leader = createEmployee(id = 10L, costCenterCode = null)
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)

            // When
            val result = service.getMembers(10L)

            // Then
            assertThat(result).isEmpty()
            verify(employeeRepository, never()).findWithEmployeeInfoByCostCenterCodeAndRole(any(), any())
        }
    }

    // ========== getAccounts ==========

    @Nested
    @DisplayName("getAccounts - 거래처 목록 조회")
    inner class GetAccountsTests {

        @Test
        @DisplayName("정상 조회 - 사용자 branchCode로 거래처 목록 반환")
        fun getAccounts_success() {
            // Given
            val employee = createEmployee(id = 10L, costCenterCode = "1234")
            val account1 = createAccount(id = 1, sfid = "ACC_001", name = "이마트 강남점", branchCode = "1234")
            val account2 = createAccount(id = 2, sfid = "ACC_002", name = "홈플러스 역삼점", branchCode = "1234")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(employee)
            whenever(accountRepository.findByBranchCodeAndAccountGroupIn("1234", listOf("1010", "1000")))
                .thenReturn(listOf(account1, account2))

            // When
            val result = service.getAccounts(10L, null)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].accountId).isEqualTo(1)
            assertThat(result[1].accountId).isEqualTo(2)
        }

        @Test
        @DisplayName("branch_code 파라미터 지정 - 지정 지점의 거래처 반환")
        fun getAccounts_withBranchCode() {
            // Given
            val account = createAccount(id = 1, sfid = "ACC_001", name = "롯데마트 잠실점", branchCode = "5678")

            whenever(accountRepository.findByBranchCodeAndAccountGroupIn("5678", listOf("1010", "1000")))
                .thenReturn(listOf(account))

            // When
            val result = service.getAccounts(10L, "5678")

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].accountId).isEqualTo(1)
            verify(employeeRepository, never()).findWithEmployeeInfoById(any())
        }
    }

    // ========== getMonthlySchedulesWithSummary ==========

    @Nested
    @DisplayName("getMonthlySchedulesWithSummary - 월간 일정 + 일별 요약 통합 조회")
    inner class GetMonthlySchedulesWithSummaryTests {

        @Test
        @DisplayName("정상 조회 - employeeIds 지정 시 schedules와 dailySummary 모두 반환")
        fun getMonthlySchedulesWithSummary_byEmployeeIds() {
            // Given
            val date = LocalDate.of(2026, 4, 1)
            val displaySchedule = createSchedule(id = 1L, employeeId = 1L, employeeCode = "20030001", employeeName = "홍길동", workingDate = date, workingType = "근무", workingCategory1 = "진열")
            val displayWithCommute = createSchedule(id = 2L, workingDate = date, workingType = "근무", workingCategory1 = "진열", commuteLogId = "CL001")
            val promotionSchedule = createSchedule(id = 3L, workingDate = date, workingType = "근무", workingCategory1 = "행사")
            val promotionWithCommute = createSchedule(id = 4L, workingDate = date, workingType = "근무", workingCategory1 = "행사", commuteLogId = "CL002")
            val annualLeave = createSchedule(id = 5L, workingDate = date, workingType = "연차", workingCategory1 = null)
            val compensatoryLeave = createSchedule(id = 6L, workingDate = date, workingType = "대휴", workingCategory1 = null)

            val allSchedules = listOf(displaySchedule, displayWithCommute, promotionSchedule, promotionWithCommute, annualLeave, compensatoryLeave)

            whenever(teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(1L)),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30))
            )).thenReturn(allSchedules)

            // When
            val result = service.getMonthlySchedulesWithSummary(1L, 2026, 4, listOf(1L), null)

            // Then - schedules
            assertThat(result.schedules).hasSize(6)
            assertThat(result.schedules[0].employeeCode).isEqualTo("20030001")
            assertThat(result.schedules[0].employeeName).isEqualTo("홍길동")

            // Then - dailySummary
            assertThat(result.dailySummary).hasSize(1)
            val summary = result.dailySummary[0]
            assertThat(summary.date).isEqualTo("2026-04-01")
            assertThat(summary.displayExpected).isEqualTo(2)
            assertThat(summary.displayActual).isEqualTo(1)
            assertThat(summary.promotionExpected).isEqualTo(2)
            assertThat(summary.promotionActual).isEqualTo(1)
            assertThat(summary.annualLeave).isEqualTo(1)
            assertThat(summary.compensatoryLeave).isEqualTo(1)
        }

        @Test
        @DisplayName("accountIds 지정 - 거래처 필터로 일정 반환")
        fun getMonthlySchedulesWithSummary_byAccountIds() {
            // Given
            val schedule = createSchedule(id = 1L, accountId = 10)

            whenever(teamMemberScheduleRepository.findMonthlyByAccountIds(
                eq(listOf(10)),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30))
            )).thenReturn(listOf(schedule))

            // When
            val result = service.getMonthlySchedulesWithSummary(1L, 2026, 4, null, listOf(10))

            // Then
            assertThat(result.schedules).hasSize(1)
            assertThat(result.schedules[0].accountId).isEqualTo(10)
            assertThat(result.dailySummary).hasSize(1)
        }

        @Test
        @DisplayName("필터 없이 조회 - 빈 배열 반환")
        fun getMonthlySchedulesWithSummary_noFilter_returnsEmpty() {
            // When
            val result = service.getMonthlySchedulesWithSummary(1L, 2026, 4, null, null)

            // Then
            assertThat(result.schedules).isEmpty()
            assertThat(result.dailySummary).isEmpty()
            verifyNoInteractions(teamMemberScheduleRepository)
        }
    }

    // ========== createSchedule ==========

    @Nested
    @DisplayName("createSchedule - 일정 등록")
    inner class CreateScheduleTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> 일정 ID 반환")
        fun createSchedule_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(emptyList())
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(account))
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>())).thenReturn(savedSchedule)

            // When
            val result = service.createSchedule(10L, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
            verify(teamMemberScheduleRepository).save(any<TeamMemberSchedule>())
        }

        @Test
        @DisplayName("휴직 사원 등록 - EMPLOYEE_ON_LEAVE")
        fun createSchedule_employeeOnLeave() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "휴직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무"
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleEmployeeOnLeaveException::class.java)
        }

        @Test
        @DisplayName("퇴직 사원 등록 - EMPLOYEE_RESIGNED")
        fun createSchedule_employeeResigned() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "퇴직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무"
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleEmployeeResignedException::class.java)
        }

        @Test
        @DisplayName("고정 중복 등록 (D1) - SCHEDULE_CONFLICT")
        fun createSchedule_fixedDuplicate() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingSchedule = createSchedule(id = 50L, workingCategory3 = "고정")

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existingSchedule))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("고정과 격고/순회 공존 불가 (D2/D3) - SCHEDULE_CONFLICT")
        fun createSchedule_fixedWithOtherTypes() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingSchedule = createSchedule(id = 50L, workingCategory3 = "격고")

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existingSchedule))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("격고 3건 초과 (D4) - SCHEDULE_CONFLICT")
        fun createSchedule_alternateExceedsLimit() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existing1 = createSchedule(id = 50L, workingCategory3 = "격고")
            val existing2 = createSchedule(id = 51L, workingCategory3 = "격고")

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existing1, existing2))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
        }

        @Test
        @DisplayName("순회+격고1 존재, 격고 추가 - SCHEDULE_CONFLICT")
        fun createSchedule_patrolAndAlternateExist_alternateBlocked() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val existingPatrol = createSchedule(id = 50L, workingCategory3 = "순회")
            val existingAlternate = createSchedule(id = 51L, workingCategory3 = "격고")

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existingPatrol, existingAlternate))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleConflictException::class.java)
                .hasMessageContaining("순회 일정이 존재하므로 격고는 1건만 등록 가능합니다")
        }

        @Test
        @DisplayName("순회만 존재, 격고 추가 - 성공")
        fun createSchedule_patrolOnly_alternateAllowed() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val existingPatrol = createSchedule(id = 50L, workingCategory3 = "순회")
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existingPatrol))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(account))
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>())).thenReturn(savedSchedule)

            // When
            val result = service.createSchedule(10L, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("격고1만 존재(순회 없음), 격고 추가 - 성공")
        fun createSchedule_alternateOnly_secondAlternateAllowed() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val account = createAccount(sfid = "ACC_SFID_001")
            val existingAlternate = createSchedule(id = 50L, workingCategory3 = "격고")
            val savedSchedule = createSchedule(id = 100L)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "격고",
                accountId = 1
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, LocalDate.of(2026, 4, 1)))
                .thenReturn(listOf(existingAlternate))
            whenever(accountRepository.findById(1)).thenReturn(Optional.of(account))
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>())).thenReturn(savedSchedule)

            // When
            val result = service.createSchedule(10L, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("근무+거래처 없음 생성 - ACCOUNT_REQUIRED")
        fun createSchedule_workTypeWorkWithoutAccount() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "근무",
                accountId = null
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("연차+거래처 없음 생성 - 정상 허용")
        fun createSchedule_leaveTypeWithoutAccount_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val savedSchedule = createSchedule(id = 100L, workingType = "연차", workingCategory1 = null, workingCategory3 = null, accountId = null)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "연차",
                accountId = null
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>())).thenReturn(savedSchedule)

            // When
            val result = service.createSchedule(10L, request)

            // Then
            assertThat(result.id).isEqualTo(100L)
        }

        @Test
        @DisplayName("대휴+거래처 없음 생성 - 정상 허용")
        fun createSchedule_substituteLeaveWithoutAccount_success() {
            // Given
            val employee = createEmployee(employeeCode = "20030001", status = "재직")
            val leader = createEmployee(id = 10L, sfid = "LEADER_SFID")
            val savedSchedule = createSchedule(id = 101L, workingType = "대휴", workingCategory1 = null, workingCategory3 = null, accountId = null)

            val request = TeamScheduleCreateRequest(
                employeeCode = "20030001",
                workingDate = "2026-04-01",
                workingType = "대휴",
                accountId = null
            )

            whenever(employeeRepository.findByEmployeeCode("20030001")).thenReturn(Optional.of(employee))
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>())).thenReturn(savedSchedule)

            // When
            val result = service.createSchedule(10L, request)

            // Then
            assertThat(result.id).isEqualTo(101L)
        }

        @Test
        @DisplayName("미존재 사원 등록 - NOT_FOUND")
        fun createSchedule_employeeNotFound() {
            // Given
            val request = TeamScheduleCreateRequest(
                employeeCode = "NONEXISTENT",
                workingDate = "2026-04-01",
                workingType = "근무"
            )

            whenever(employeeRepository.findByEmployeeCode("NONEXISTENT")).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { service.createSchedule(10L, request) }
                .isInstanceOf(TeamScheduleEmployeeNotFoundException::class.java)
        }
    }

    // ========== updateSchedule ==========

    @Nested
    @DisplayName("updateSchedule - 일정 수정")
    inner class UpdateScheduleTests {

        @Test
        @DisplayName("정상 수정 - 거래처 변경")
        fun updateSchedule_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )
            val newAccount = createAccount(id = 2, sfid = "ACC_NEW")

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 2
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(false)
            whenever(accountRepository.findById(2)).thenReturn(Optional.of(newAccount))

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.account?.id).isEqualTo(2)
        }

        @Test
        @DisplayName("미존재 일정 수정 - NOT_FOUND")
        fun updateSchedule_notFound() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무"
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 999L, request) }
                .isInstanceOf(TeamScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (일반 사용자) - DISPLAY_MASTER_LINK_CONSTRAINT")
        fun updateSchedule_displayMasterLinked_forbidden() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 100L, request) }
                .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (시스템관리자) - 수정 성공")
        fun updateSchedule_displayMasterLinked_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = UserRole.SYSTEM_ADMIN)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(admin)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.updateSchedule(10L, 100L, request)

            // Then (no exception thrown)
            verify(displayWorkScheduleRepository, never()).existsConfirmedByEmployeeAndAccountAndDate(any(), any(), any())
        }

        @Test
        @DisplayName("진열마스터 연결 일정 수정 (영업지원실) - 수정 성공")
        fun updateSchedule_displayMasterLinked_salesSupport_success() {
            // Given
            val salesSupport = createEmployee(id = 10L, role = UserRole.SALES_SUPPORT)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(salesSupport)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.updateSchedule(10L, 100L, request)

            // Then (no exception thrown)
            verify(displayWorkScheduleRepository, never()).existsConfirmedByEmployeeAndAccountAndDate(any(), any(), any())
        }

        @Test
        @DisplayName("과거 진열 일정 날짜 변경 - PAST_DATE_CHANGE_NOT_ALLOWED")
        fun updateSchedule_pastDate_displaySchedule_dateChange() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val yesterday = LocalDate.now().minusDays(1)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = LocalDate.now().plusDays(1).toString(),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, yesterday))
                .thenReturn(false)

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 100L, request) }
                .isInstanceOf(TeamSchedulePastDateChangeException::class.java)
        }

        @Test
        @DisplayName("과거 근무 일정 날짜 변경 (카테고리 null) - PAST_DATE_CHANGE_NOT_ALLOWED")
        fun updateSchedule_pastDate_nullCategory_dateChange() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val threeDaysAgo = LocalDate.now().minusDays(3)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = threeDaysAgo,
                workingType = "근무",
                workingCategory1 = null,
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = LocalDate.now().plusDays(1).toString(),
                workingType = "근무",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 100L, request) }
                .isInstanceOf(TeamSchedulePastDateChangeException::class.java)
        }

        @Test
        @DisplayName("과거 행사 일정 날짜 변경 - 수정 성공 (행사 예외)")
        fun updateSchedule_pastDate_eventSchedule_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val yesterday = LocalDate.now().minusDays(1)
            val tomorrow = LocalDate.now().plusDays(1)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = "근무",
                workingCategory1 = "행사",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = tomorrow.toString(),
                workingType = "근무",
                workingCategory1 = "행사",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(tomorrow)
        }

        @Test
        @DisplayName("과거 일자 비날짜 필드 수정 - 수정 성공")
        fun updateSchedule_pastDate_nonDateFieldChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val yesterday = LocalDate.now().minusDays(1)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = yesterday,
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = yesterday.toString(),
                workingType = "근무",
                workingCategory1 = "행사",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, yesterday))
                .thenReturn(false)

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.workingCategory1).isEqualTo("행사")
        }

        @Test
        @DisplayName("오늘 일자 날짜 변경 - 수정 성공")
        fun updateSchedule_todayDate_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val today = LocalDate.now()
            val tomorrow = LocalDate.now().plusDays(1)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = today,
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = tomorrow.toString(),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, today))
                .thenReturn(false)
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, tomorrow))
                .thenReturn(emptyList())

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(tomorrow)
        }

        @Test
        @DisplayName("미래 일자 날짜 변경 - 수정 성공")
        fun updateSchedule_futureDate_dateChange_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val tomorrow = LocalDate.now().plusDays(1)
            val dayAfter = LocalDate.now().plusDays(2)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = tomorrow,
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = dayAfter.toString(),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            whenever(adminEmployeeHolder.employee).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, tomorrow))
                .thenReturn(false)
            whenever(teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(1L, dayAfter))
                .thenReturn(emptyList())

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.workingDate).isEqualTo(dayAfter)
        }

        @Test
        @DisplayName("비진열 일정 수정 (일반 사용자) - 수정 성공")
        fun updateSchedule_nonDisplay_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "행사",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "행사",
                accountId = 1
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.updateSchedule(10L, 100L, request)

            // Then (no exception, no display master check)
            verify(displayWorkScheduleRepository, never()).existsConfirmedByEmployeeAndAccountAndDate(any(), any(), any())
        }

        @Test
        @DisplayName("근무→근무 수정 거래처 미지정 - ACCOUNT_REQUIRED")
        fun updateSchedule_workTypeWorkWithoutAccount() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                workingCategory1 = "진열",
                accountId = null
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(false)

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 100L, request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("연차→근무 수정 거래처 미지정 - ACCOUNT_REQUIRED")
        fun updateSchedule_leaveToWorkWithoutAccount() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "연차",
                workingCategory1 = null,
                workingCategory3 = null,
                accountId = null
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "근무",
                accountId = null
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When & Then
            assertThatThrownBy { service.updateSchedule(10L, 100L, request) }
                .isInstanceOf(TeamScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("근무→연차 수정 거래처 제거 - 정상 허용")
        fun updateSchedule_workToLeaveWithoutAccount_success() {
            // Given
            val currentUser = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(
                id = 100L,
                employeeId = 1L,
                workingDate = LocalDate.of(2026, 4, 1),
                workingType = "근무",
                workingCategory1 = "진열",
                workingCategory3 = "고정",
                accountId = 1
            )

            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-04-01",
                workingType = "연차",
                accountId = null
            )

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(currentUser)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(false)

            // When
            service.updateSchedule(10L, 100L, request)

            // Then
            assertThat(schedule.workingType).isEqualTo("연차")
            assertThat(schedule.account).isNull()
        }
    }

    // ========== deleteSchedule ==========

    @Nested
    @DisplayName("deleteSchedule - 일정 삭제")
    inner class DeleteScheduleTests {

        @Test
        @DisplayName("정상 삭제 - 조장 계정으로 삭제 성공")
        fun deleteSchedule_success() {
            // Given
            val leader = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(id = 100L)

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(false)

            // When
            service.deleteSchedule(10L, 100L)

            // Then
            verify(teamMemberScheduleRepository).delete(schedule)
        }

        @Test
        @DisplayName("지점장 삭제 시도 - FORBIDDEN")
        fun deleteSchedule_forbiddenForBranchManager() {
            // Given
            val branchManager = createEmployee(id = 10L, role = UserRole.BRANCH_MANAGER)
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(branchManager)

            // When & Then
            assertThatThrownBy { service.deleteSchedule(10L, 100L) }
                .isInstanceOf(TeamScheduleDeleteForbiddenException::class.java)
            verify(teamMemberScheduleRepository, never()).delete(any())
        }

        @Test
        @DisplayName("미존재 일정 삭제 - NOT_FOUND")
        fun deleteSchedule_notFound() {
            // Given
            val leader = createEmployee(id = 10L, role = UserRole.LEADER)
            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { service.deleteSchedule(10L, 999L) }
                .isInstanceOf(TeamScheduleNotFoundException::class.java)
        }

        @Test
        @DisplayName("근무등록 완료 일정 삭제 (일반 사용자) - WORK_REPORT_DELETE_CONSTRAINT")
        fun deleteSchedule_workReportCompleted_forbidden() {
            // Given
            val leader = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(id = 100L, commuteLogId = "CL001")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When & Then
            assertThatThrownBy { service.deleteSchedule(10L, 100L) }
                .isInstanceOf(TeamScheduleWorkReportDeleteException::class.java)
            verify(teamMemberScheduleRepository, never()).delete(any())
        }

        @Test
        @DisplayName("근무등록 완료 일정 삭제 (시스템관리자) - 삭제 성공")
        fun deleteSchedule_workReportCompleted_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = UserRole.SYSTEM_ADMIN)
            val schedule = createSchedule(id = 100L, commuteLogId = "CL001")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(admin)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.deleteSchedule(10L, 100L)

            // Then
            verify(teamMemberScheduleRepository).delete(schedule)
        }

        @Test
        @DisplayName("미등록 일정 삭제 (일반 사용자) - 삭제 성공")
        fun deleteSchedule_noWorkReport_success() {
            // Given
            val leader = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(id = 100L, commuteLogId = null)

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(false)

            // When
            service.deleteSchedule(10L, 100L)

            // Then
            verify(teamMemberScheduleRepository).delete(schedule)
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (일반 사용자) - DISPLAY_MASTER_LINK_CONSTRAINT")
        fun deleteSchedule_displayMasterLinked_forbidden() {
            // Given
            val leader = createEmployee(id = 10L, role = UserRole.LEADER)
            val schedule = createSchedule(id = 100L, commuteLogId = null, workingCategory1 = "진열")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(leader)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))
            whenever(displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(1L, 1, LocalDate.of(2026, 4, 1)))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { service.deleteSchedule(10L, 100L) }
                .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)
            verify(teamMemberScheduleRepository, never()).delete(any())
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (시스템관리자) - 삭제 성공")
        fun deleteSchedule_displayMasterLinked_systemAdmin_success() {
            // Given
            val admin = createEmployee(id = 10L, role = UserRole.SYSTEM_ADMIN)
            val schedule = createSchedule(id = 100L, commuteLogId = null, workingCategory1 = "진열")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(admin)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.deleteSchedule(10L, 100L)

            // Then
            verify(teamMemberScheduleRepository).delete(schedule)
            verify(displayWorkScheduleRepository, never()).existsConfirmedByEmployeeAndAccountAndDate(any(), any(), any())
        }

        @Test
        @DisplayName("진열마스터 연결 일정 삭제 (영업지원실) - 삭제 성공")
        fun deleteSchedule_displayMasterLinked_salesSupport_success() {
            // Given
            val salesSupport = createEmployee(id = 10L, role = UserRole.SALES_SUPPORT)
            val schedule = createSchedule(id = 100L, commuteLogId = null, workingCategory1 = "진열")

            whenever(employeeRepository.findWithEmployeeInfoById(10L)).thenReturn(salesSupport)
            whenever(teamMemberScheduleRepository.findById(100L)).thenReturn(Optional.of(schedule))

            // When
            service.deleteSchedule(10L, 100L)

            // Then
            verify(teamMemberScheduleRepository).delete(schedule)
            verify(displayWorkScheduleRepository, never()).existsConfirmedByEmployeeAndAccountAndDate(any(), any(), any())
        }
    }
}
