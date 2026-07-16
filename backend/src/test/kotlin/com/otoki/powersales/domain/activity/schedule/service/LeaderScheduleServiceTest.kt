package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderCalendarDay
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleInvalidWorkCategory2Exception
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleMissingWorkCategory3Exception
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.service.PromotionSchedulesUpsertHelper
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleCascadeHelper
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.LeaderScheduleService
import com.otoki.powersales.domain.activity.schedule.service.ScheduleConflictValidator
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleOwnerResolver
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@DisplayName("LeaderScheduleService 테스트")
class LeaderScheduleServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val accountRepository: AccountRepository = mockk()

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()

    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator = mockk()

    private val scheduleConflictValidator: ScheduleConflictValidator = mockk(relaxUnitFun = true)

    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver = mockk()

    private val promotionEmployeeRepository: PromotionEmployeeRepository = mockk()

    private val promotionSchedulesUpsertHelper: PromotionSchedulesUpsertHelper = mockk()

    private val teamMemberScheduleCascadeHelper: TeamMemberScheduleCascadeHelper = mockk()

    // 일별현황/월간캘린더 계산 코어 — 계산 로직 자체는 TeamDailyStatusCalculatorTest 가 검증한다.
    // 여기서는 조장 권한/팀원 검증 + 대상 인원 스코프(costCenterCode) 후 위임 여부만 확인한다.
    private val teamDailyStatusCalculator: TeamDailyStatusCalculator = mockk()

    private val adminEmployeeCredentialService: AdminEmployeeCredentialService = mockk()

    private val leaderScheduleService = LeaderScheduleService(
        employeeRepository,
        accountRepository,
        teamMemberScheduleRepository,
        teamMemberScheduleNameGenerator,
        scheduleConflictValidator,
        teamMemberScheduleOwnerResolver,
        promotionEmployeeRepository,
        promotionSchedulesUpsertHelper,
        teamMemberScheduleCascadeHelper,
        teamDailyStatusCalculator,
        adminEmployeeCredentialService,
    )

    init {
        every { teamMemberScheduleNameGenerator.next() } returns "TS00000001"
        every { teamMemberScheduleOwnerResolver.resolveOwner(any()) } returns null
    }

    @Nested
    @DisplayName("createTeamMemberSchedule - 조장 대리 일정 등록")
    inner class CreateTeamMemberScheduleTests {

        @Test
        @DisplayName("정상 등록 - 조장이 본인 팀원의 근무 일정 등록 -> proxy_registered_by 저장")
        fun create_success() {
            // Given
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "활동")
            val account = createAccount(id = 90234, branchCode = "C001", accountGroup = "1000")
            val request = createRequest()

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)
            every { accountRepository.findById(account.id) } returns Optional.of(account)
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } answers { firstArg<TeamMemberSchedule>() }

            // When
            val result = leaderScheduleService.createTeamMemberSchedule(leader.id, request)

            // Then
            assertThat(result.targetEmployeeId).isEqualTo(target.id)
            assertThat(result.workingDate).isEqualTo("2026-05-15")
            assertThat(result.workingType).isEqualTo("근무")
            assertThat(result.workingCategory3).isEqualTo("고정")
            assertThat(result.proxyRegisteredBy).isEqualTo(leader.id)
            verify { scheduleConflictValidator.validateConflicts(
                eq(target.id), eq(LocalDate.parse("2026-05-15")), eq(WorkingType.WORK), eq(account.id), eq(WorkingCategory3.FIXED)
            ) }
        }

        @Test
        @DisplayName("실패 - 등록자 미존재 -> EmployeeNotFoundException")
        fun create_registrantNotFound() {
            every { employeeRepository.findById(4001L) } returns Optional.empty()

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(4001L, createRequest()) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 비조장 사용자 -> NOT_LEADER")
        fun create_notLeader() {
            val nonLeader = createEmployee(id = 4001, authority = AppAuthority.WOMAN, costCenterCode = "C001")
            every { employeeRepository.findById(nonLeader.id) } returns Optional.of(nonLeader)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(nonLeader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleNotLeaderException::class.java)
        }

        @Test
        @DisplayName("실패 - 근무 외 working_type -> INVALID_WORKING_TYPE")
        fun create_invalidWorkingType() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)

            val request = createRequest(workingType = "연차")
            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, request) }
                .isInstanceOf(LeaderScheduleInvalidWorkingTypeException::class.java)
        }

        @Test
        @DisplayName("실패 - 전담 외 working_category2 -> INVALID_WORK_CATEGORY2")
        fun create_invalidWorkCategory2() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)

            val request = createRequest(workingCategory2 = "파트")
            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, request) }
                .isInstanceOf(LeaderScheduleInvalidWorkCategory2Exception::class.java)
        }

        @Test
        @DisplayName("실패 - 카테고리3 잘못된 값 -> MISSING_WORK_CATEGORY3")
        fun create_invalidWorkCategory3() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)

            val request = createRequest(workingCategory3 = "기타")
            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, request) }
                .isInstanceOf(LeaderScheduleMissingWorkCategory3Exception::class.java)
        }

        @Test
        @DisplayName("실패 - 거래처 누락 -> ACCOUNT_REQUIRED")
        fun create_accountRequired() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)

            val request = createRequest(accountId = null)
            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, request) }
                .isInstanceOf(LeaderScheduleAccountRequiredException::class.java)
        }

        @Test
        @DisplayName("실패 - 미존재 직원 -> TARGET_EMPLOYEE_NOT_FOUND")
        fun create_targetNotFound() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(5012L) } returns Optional.empty()

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleTargetEmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 다른 팀의 직원 등록 -> NOT_TEAM_MEMBER")
        fun create_notTeamMember() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C002", status = "활동")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleNotTeamMemberException::class.java)
        }

        @Test
        @DisplayName("실패 - 휴직 직원 -> TARGET_EMPLOYEE_INACTIVE")
        fun create_targetOnLeave() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "휴직")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleTargetEmployeeInactiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 퇴직 직원 -> TARGET_EMPLOYEE_INACTIVE")
        fun create_targetRetired() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "퇴직")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleTargetEmployeeInactiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 거래처 미존재 -> NOT_LEADER_ACCOUNT")
        fun create_accountNotFound() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "활동")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)
            every { accountRepository.findById(any<Long>()) } returns Optional.empty()

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleNotLeaderAccountException::class.java)
        }

        @Test
        @DisplayName("실패 - 다른 지점 거래처 -> NOT_LEADER_ACCOUNT")
        fun create_accountWrongBranch() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "활동")
            val account = createAccount(id = 90234, branchCode = "C999", accountGroup = "1000")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)
            every { accountRepository.findById(account.id) } returns Optional.of(account)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleNotLeaderAccountException::class.java)
        }

        @Test
        @DisplayName("실패 - 거래처 그룹 외 -> NOT_LEADER_ACCOUNT")
        fun create_accountWrongGroup() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val target = createEmployee(id = 5012, authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "활동")
            val account = createAccount(id = 90234, branchCode = "C001", accountGroup = "9999")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findById(target.id) } returns Optional.of(target)
            every { accountRepository.findById(account.id) } returns Optional.of(account)

            assertThatThrownBy { leaderScheduleService.createTeamMemberSchedule(leader.id, createRequest()) }
                .isInstanceOf(LeaderScheduleNotLeaderAccountException::class.java)
            verify(exactly = 0) { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }
    }

    @Nested
    @DisplayName("getTeamMembers - 본인 팀원 목록 조회")
    inner class GetTeamMembersTests {

        @Test
        @DisplayName("성공 - 조장·지점장 제외(역필터) + 퇴직 제외(휴직 포함) + 이름 가나다순 (레거시 empSearch parity)")
        fun getTeamMembers_success() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val m1 = createEmployee(id = 5012, employeeCode = "20300002", authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "활동", name = "최여사")
            val m2 = createEmployee(id = 5013, employeeCode = "20300001", authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "휴직", name = "김여사")
            val resigned = createEmployee(id = 5014, employeeCode = "20300003", authority = AppAuthority.WOMAN, costCenterCode = "C001", status = "퇴직", name = "박여사")

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            // 서비스가 조장·지점장 제외 finder 를 호출 → 여사원만 반환되는 상황을 모킹
            every {
                employeeRepository.findByCostCenterCodeAndRoleNotIn(
                    "C001",
                    listOf(AppAuthority.LEADER, AppAuthority.BRANCH_MANAGER)
                )
            } returns listOf(m1, m2, resigned)

            val result = leaderScheduleService.getTeamMembers(leader.id)

            // 퇴직(박여사) 제외 → 2명, 이름순(김 < 최)
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("김여사")
            assertThat(result[0].status).isEqualTo("휴직")
            assertThat(result[1].name).isEqualTo("최여사")
        }

        @Test
        @DisplayName("실패 - 비조장 -> NOT_LEADER")
        fun getTeamMembers_notLeader() {
            val nonLeader = createEmployee(id = 4001, authority = AppAuthority.WOMAN, costCenterCode = "C001")
            every { employeeRepository.findById(nonLeader.id) } returns Optional.of(nonLeader)

            assertThatThrownBy { leaderScheduleService.getTeamMembers(nonLeader.id) }
                .isInstanceOf(LeaderScheduleNotLeaderException::class.java)
        }
    }

    @Nested
    @DisplayName("getAccounts - 본인 거래처 목록 조회")
    inner class GetAccountsTests {

        @Test
        @DisplayName("성공 - 조장의 거래처가 name ASC 정렬되어 반환")
        fun getAccounts_success() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val a1 = createAccount(id = 100, branchCode = "C001", accountGroup = "1000", name = "ZebraMart")
            val a2 = createAccount(id = 101, branchCode = "C001", accountGroup = "1010", name = "AlphaMart")

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                "C001", listOf("1000", "1010"), true
            ) } returns listOf(a1, a2)

            val result = leaderScheduleService.getAccounts(leader.id, null)

            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("AlphaMart")
            assertThat(result[1].name).isEqualTo("ZebraMart")
        }

        @Test
        @DisplayName("성공 - keyword 부분 일치 필터")
        fun getAccounts_withKeyword() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val a1 = createAccount(id = 100, branchCode = "C001", accountGroup = "1000", name = "ZebraMart", address1 = "Seoul")
            val a2 = createAccount(id = 101, branchCode = "C001", accountGroup = "1010", name = "AlphaMart", address1 = "Busan")

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                "C001", listOf("1000", "1010"), true
            ) } returns listOf(a1, a2)

            val result = leaderScheduleService.getAccounts(leader.id, "alpha")

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("AlphaMart")
        }

        @Test
        @DisplayName("성공 - keyword 거래처코드(externalKey) 일치, 주소는 미검색 (레거시 teamleaderAccList 정합)")
        fun getAccounts_keywordMatchesCodeNotAddress() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            // a1: 거래처코드에 키워드 포함 → 매칭
            val a1 = createAccount(id = 100, branchCode = "C001", accountGroup = "1000", name = "ZebraMart", address1 = "Seoul", externalKey = "1000777")
            // a2: 주소에만 키워드 포함 → 레거시는 주소 미검색이므로 제외
            val a2 = createAccount(id = 101, branchCode = "C001", accountGroup = "1010", name = "AlphaMart", address1 = "777번지", externalKey = "1000111")

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                "C001", listOf("1000", "1010"), true
            ) } returns listOf(a1, a2)

            val result = leaderScheduleService.getAccounts(leader.id, "777")

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("ZebraMart")
        }

        @Test
        @DisplayName("실패 - 비조장 -> NOT_LEADER")
        fun getAccounts_notLeader() {
            val nonLeader = createEmployee(id = 4001, authority = AppAuthority.WOMAN, costCenterCode = "C001")
            every { employeeRepository.findById(nonLeader.id) } returns Optional.of(nonLeader)

            assertThatThrownBy { leaderScheduleService.getAccounts(nonLeader.id, null) }
                .isInstanceOf(LeaderScheduleNotLeaderException::class.java)
        }
    }

    @Nested
    @DisplayName("getDailyStatus - 조장 권한/스코프 검증 후 계산기 위임")
    inner class GetDailyStatusTests {

        @Test
        @DisplayName("성공 - 조장 costCenterCode 인원으로 계산기에 위임")
        fun getDailyStatus_delegatesWithTeamScope() {
            val date = LocalDate.of(2026, 6, 10)
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val w1 = createEmployee(id = 10, authority = AppAuthority.WOMAN, costCenterCode = "C001", name = "김여사")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findByCostCenterCodeIn(listOf("C001")) } returns listOf(leader, w1)
            val expected = emptyDailyStatus(date)
            every { teamDailyStatusCalculator.computeDailyStatus(listOf(4001L, 10L), date) } returns expected

            val result = leaderScheduleService.getDailyStatus(leader.id, date)

            assertThat(result).isSameAs(expected)
            verify { teamDailyStatusCalculator.computeDailyStatus(listOf(4001L, 10L), date) }
        }

        @Test
        @DisplayName("costCenterCode 없음 -> 빈 현황 (계산기 미호출)")
        fun getDailyStatus_noCostCenter() {
            val date = LocalDate.of(2026, 6, 10)
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = null)
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { teamDailyStatusCalculator.emptyDailyStatus(date) } returns emptyDailyStatus(date)

            val result = leaderScheduleService.getDailyStatus(leader.id, date)

            assertThat(result.displayWorkers).isEmpty()
        }

        @Test
        @DisplayName("실패 - 비조장 -> NOT_LEADER")
        fun getDailyStatus_notLeader() {
            val nonLeader = createEmployee(id = 4001, authority = AppAuthority.WOMAN, costCenterCode = "C001")
            every { employeeRepository.findById(nonLeader.id) } returns Optional.of(nonLeader)

            assertThatThrownBy { leaderScheduleService.getDailyStatus(nonLeader.id, LocalDate.of(2026, 6, 10)) }
                .isInstanceOf(LeaderScheduleNotLeaderException::class.java)
        }
    }

    @Nested
    @DisplayName("getMonthlyCalendar - 조장 권한/팀원 검증 후 계산기 위임")
    inner class GetMonthlyCalendarTests {

        @Test
        @DisplayName("전체 모드 - 조 전체 인원으로 일자별 계산기 위임 + null 제외")
        fun monthly_all_success() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val w1 = createEmployee(id = 10, authority = AppAuthority.WOMAN, costCenterCode = "C001", name = "김여사")
            val teamIds = listOf(4001L, 10L)
            val date10 = LocalDate.of(2026, 6, 10)

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findByCostCenterCodeIn(listOf("C001")) } returns listOf(leader, w1)
            // 6/10 만 데이터, 나머지 날짜는 null
            every { teamDailyStatusCalculator.computeCalendarDay(teamIds, any()) } returns null
            every { teamDailyStatusCalculator.computeCalendarDay(teamIds, date10) } returns
                LeaderCalendarDay(date = "2026-06-10", total = 2, attended = 1)

            val result = leaderScheduleService.getMonthlyCalendar(leader.id, null, 2026, 6)

            assertThat(result.year).isEqualTo(2026)
            assertThat(result.month).isEqualTo(6)
            assertThat(result.days).hasSize(1)
            assertThat(result.days[0].date).isEqualTo("2026-06-10")
            assertThat(result.days[0].total).isEqualTo(2)
            assertThat(result.days[0].attended).isEqualTo(1)
        }

        @Test
        @DisplayName("개인 모드 - 대상 조원만 계산기 위임")
        fun monthly_personal_success() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            val w1 = createEmployee(id = 10, authority = AppAuthority.WOMAN, costCenterCode = "C001", name = "김여사")
            val targetIds = listOf(10L)
            val date10 = LocalDate.of(2026, 6, 10)

            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findByCostCenterCodeIn(listOf("C001")) } returns
                listOf(leader, w1, createEmployee(id = 11, authority = AppAuthority.WOMAN, costCenterCode = "C001"))
            every { teamDailyStatusCalculator.computeCalendarDay(targetIds, any()) } returns null
            every { teamDailyStatusCalculator.computeCalendarDay(targetIds, date10) } returns
                LeaderCalendarDay(date = "2026-06-10", total = 1, attended = 1)

            val result = leaderScheduleService.getMonthlyCalendar(leader.id, 10L, 2026, 6)

            assertThat(result.days).hasSize(1)
            assertThat(result.days[0].total).isEqualTo(1)
            assertThat(result.days[0].attended).isEqualTo(1)
        }

        @Test
        @DisplayName("실패 - 개인 모드 대상이 본인 팀원 아님 -> NOT_TEAM_MEMBER")
        fun monthly_personal_notTeamMember() {
            val leader = createEmployee(id = 4001, authority = AppAuthority.LEADER, costCenterCode = "C001")
            every { employeeRepository.findById(leader.id) } returns Optional.of(leader)
            every { employeeRepository.findByCostCenterCodeIn(listOf("C001")) } returns listOf(leader)

            assertThatThrownBy { leaderScheduleService.getMonthlyCalendar(leader.id, 999L, 2026, 6) }
                .isInstanceOf(LeaderScheduleNotTeamMemberException::class.java)
        }
    }

    // ========== Helpers ==========

    private fun emptyDailyStatus(date: LocalDate): LeaderDailyStatusResponse =
        LeaderDailyStatusResponse(
            date = date.toString(),
            summary = LeaderDailyStatusSummary(0, 0, 0, 0, 0),
            displayWorkers = emptyList(),
            eventWorkers = emptyList(),
            annualLeaveWorkers = emptyList(),
        )

    private fun createEmployee(
        id: Long,
        employeeCode: String = "20030${id}",
        authority: String,
        costCenterCode: String?,
        status: String? = "활동",
        name: String = "사원$id"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        password = "encoded",
        role = authority,
        costCenterCode = costCenterCode,
        status = status
    )

    private fun createAccount(
        id: Long,
        branchCode: String?,
        accountGroup: String?,
        name: String? = "거래처$id",
        address1: String? = null,
        externalKey: String? = null
    ): Account = Account(
        id = id,
        name = name,
        branchCode = branchCode,
        accountGroup = accountGroup,
        address1 = address1,
        externalKey = externalKey
    )

    private fun createRequest(
        targetEmployeeId: Long? = 5012L,
        workingDate: String = "2026-05-15",
        workingType: String = "근무",
        workingCategory2: String = "전담",
        workingCategory3: String = "고정",
        accountId: Long? = 90234,
        workingCategory1: String? = "진열"
    ): LeaderScheduleCreateRequest = LeaderScheduleCreateRequest(
        targetEmployeeId = targetEmployeeId,
        workingDate = workingDate,
        workingType = workingType,
        workingCategory2 = workingCategory2,
        workingCategory3 = workingCategory3,
        accountId = accountId,
        workingCategory1 = workingCategory1
    )
}
