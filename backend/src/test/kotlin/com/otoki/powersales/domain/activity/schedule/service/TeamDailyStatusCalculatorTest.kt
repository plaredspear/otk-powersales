package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * [TeamDailyStatusCalculator] 테스트 — 조장 일별현황/월간캘린더와 AccountViewAll 대리출근이
 * 공유하는 계산 코어. (계산 로직은 기존 LeaderScheduleService.getDailyStatus/getMonthlyCalendar
 * 에서 이관됨 — 레거시 mngDaily/calSchedule 동등성 검증.)
 */
@DisplayName("TeamDailyStatusCalculator 테스트")
class TeamDailyStatusCalculatorTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository = mockk()

    private val calculator = TeamDailyStatusCalculator(
        teamMemberScheduleRepository,
        displayWorkScheduleRepository,
        safetyCheckSubmissionRepository,
    )

    @Test
    @DisplayName("빈 인원 -> 빈 현황")
    fun computeDailyStatus_empty() {
        val result = calculator.computeDailyStatus(emptyList(), LocalDate.of(2026, 6, 10))
        assertThat(result.displayWorkers).isEmpty()
        assertThat(result.eventWorkers).isEmpty()
        assertThat(result.annualLeaveWorkers).isEmpty()
        assertThat(result.summary.displayTotal).isEqualTo(0)
    }

    @Test
    @DisplayName("진열=마스터+안전점검 제출자만, 행사=cat1 EVENT, 연차=정상표시, 요약=고유 여사원 수")
    fun computeDailyStatus_success() {
        val date = LocalDate.of(2026, 6, 10)
        val w1 = createEmployee(id = 10, name = "김여사")
        val w2 = createEmployee(id = 11, name = "박여사")
        val a1 = createAccount(id = 100, name = "마트A", externalKey = "K1")
        val a2 = createAccount(id = 101, name = "마트B", externalKey = "K2")

        val teamIds = listOf(10L, 11L)

        // 진열 마스터: w1@a1, w2@a2 (둘 다 확정·유효). 단 안전점검은 w1 만 제출 → w2 진열 제외.
        every {
            displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(teamIds, date)
        } returns listOf(
            displayMaster(employee = w1, account = a1),
            displayMaster(employee = w2, account = a2),
        )
        every {
            safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(teamIds, date)
        } returns listOf(safetySubmission(employeeId = 10L))

        // team_member_schedule: w1@a1 진열 출근(attended), w2@a2 행사, w2 연차.
        every {
            teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamIds)
        } returns listOf(
            tms(employee = w1, account = a1, type = WorkingType.WORK, cat1 = WorkingCategory1.DISPLAY, attended = true),
            tms(employee = w2, account = a2, type = WorkingType.WORK, cat1 = WorkingCategory1.EVENT, attended = false),
            tms(employee = w2, account = null, type = WorkingType.ANNUAL_LEAVE, cat1 = null, attended = false),
        )

        val result = calculator.computeDailyStatus(teamIds, date)

        assertThat(result.displayWorkers).hasSize(1)
        assertThat(result.displayWorkers[0].employeeName).isEqualTo("김여사")
        assertThat(result.displayWorkers[0].accountName).isEqualTo("마트A")
        assertThat(result.displayWorkers[0].attended).isTrue()
        assertThat(result.eventWorkers).hasSize(1)
        assertThat(result.eventWorkers[0].employeeName).isEqualTo("박여사")
        assertThat(result.eventWorkers[0].attended).isFalse()
        assertThat(result.annualLeaveWorkers).hasSize(1)
        assertThat(result.annualLeaveWorkers[0].employeeName).isEqualTo("박여사")
        assertThat(result.summary.displayTotal).isEqualTo(1)
        assertThat(result.summary.displayAttended).isEqualTo(1)
        assertThat(result.summary.eventTotal).isEqualTo(1)
        assertThat(result.summary.eventAttended).isEqualTo(0)
        assertThat(result.summary.annualLeaveCount).isEqualTo(1)
    }

    @Test
    @DisplayName("진열 정렬 = 출근완료 → 임시(미출근) → 정규(미출근) + 요약은 distinct 여사원 수")
    fun computeDailyStatus_displayOrdering() {
        val date = LocalDate.of(2026, 6, 10)
        val w1 = createEmployee(id = 10, name = "이여사")
        val w2 = createEmployee(id = 11, name = "박여사")
        val w3 = createEmployee(id = 12, name = "김여사")
        val a1 = createAccount(id = 100, name = "마트A")
        val a2 = createAccount(id = 101, name = "마트B")
        val a3 = createAccount(id = 102, name = "마트C")

        val teamIds = listOf(10L, 11L, 12L)
        every {
            displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(teamIds, date)
        } returns listOf(
            displayMaster(employee = w1, account = a1, typeOfWork5 = TypeOfWork5.REGULAR),
            displayMaster(employee = w2, account = a2, typeOfWork5 = TypeOfWork5.TEMPORARY),
            displayMaster(employee = w3, account = a3, typeOfWork5 = TypeOfWork5.REGULAR),
        )
        every {
            safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(teamIds, date)
        } returns listOf(safetySubmission(10L), safetySubmission(11L), safetySubmission(12L))
        every {
            teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamIds)
        } returns listOf(
            tms(employee = w1, account = a1, type = WorkingType.WORK, cat1 = WorkingCategory1.DISPLAY, attended = true),
        )

        val result = calculator.computeDailyStatus(teamIds, date)

        assertThat(result.displayWorkers.map { it.employeeName })
            .containsExactly("이여사", "박여사", "김여사")
        assertThat(result.summary.displayTotal).isEqualTo(3)
        assertThat(result.summary.displayAttended).isEqualTo(1)
    }

    @Test
    @DisplayName("요약 진열 총원 = distinct 여사원 수 — 한 여사원이 상시·임시 동시 보유해도 1명")
    fun computeDailyStatus_displayTotalDedupByEmployee() {
        val date = LocalDate.of(2026, 6, 24)
        val w1 = createEmployee(id = 10, name = "강숙경")
        val w2 = createEmployee(id = 11, name = "김명숙")
        val a1 = createAccount(id = 100, name = "무실점")
        val a2 = createAccount(id = 101, name = "원주점")
        val a3 = createAccount(id = 102, name = "남원원마트")

        val teamIds = listOf(10L, 11L)
        every {
            displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(teamIds, date)
        } returns listOf(
            displayMaster(employee = w1, account = a1, typeOfWork5 = TypeOfWork5.REGULAR),
            displayMaster(employee = w2, account = a2, typeOfWork5 = TypeOfWork5.TEMPORARY),
            displayMaster(employee = w2, account = a3, typeOfWork5 = TypeOfWork5.REGULAR),
        )
        every {
            safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(teamIds, date)
        } returns listOf(safetySubmission(10L), safetySubmission(11L))
        every {
            teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamIds)
        } returns emptyList()

        val result = calculator.computeDailyStatus(teamIds, date)

        assertThat(result.displayWorkers).hasSize(3)
        assertThat(result.displayWorkers.mapNotNull { it.employeeId }.toSet()).hasSize(2)
        assertThat(result.summary.displayTotal).isEqualTo(2)
    }

    @Test
    @DisplayName("computeCalendarDay - 진열(출근완료)+행사(미출근) -> total=2, attended=1")
    fun computeCalendarDay_success() {
        val date = LocalDate.of(2026, 6, 10)
        val w1 = createEmployee(id = 10, name = "김여사")
        val w2 = createEmployee(id = 11, name = "박여사")
        val a1 = createAccount(id = 100, name = "마트A")
        val a2 = createAccount(id = 101, name = "마트B")
        val teamIds = listOf(10L, 11L)

        every { teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamIds) } returns listOf(
            tms(employee = w1, account = a1, type = WorkingType.WORK, cat1 = WorkingCategory1.DISPLAY, attended = true),
            tms(employee = w2, account = a2, type = WorkingType.WORK, cat1 = WorkingCategory1.EVENT, attended = false),
        )
        every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(teamIds, date) } returns
            listOf(displayMaster(employee = w1, account = a1))
        every { safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(teamIds, date) } returns
            listOf(safetySubmission(10L))

        val day = calculator.computeCalendarDay(teamIds, date)

        assertThat(day).isNotNull
        assertThat(day!!.date).isEqualTo("2026-06-10")
        assertThat(day.total).isEqualTo(2)
        assertThat(day.attended).isEqualTo(1)
    }

    @Test
    @DisplayName("computeCalendarDay - 일정 없는 날 -> null (레거시 cnt>0 만 표시)")
    fun computeCalendarDay_empty() {
        val date = LocalDate.of(2026, 6, 11)
        val teamIds = listOf(10L)
        every { teamMemberScheduleRepository.findDailyStatusByEmployeeIds(date, teamIds) } returns emptyList()
        every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdsAndDate(teamIds, date) } returns emptyList()
        every { safetyCheckSubmissionRepository.findByEmployeeIdInAndWorkingDate(teamIds, date) } returns emptyList()

        assertThat(calculator.computeCalendarDay(teamIds, date)).isNull()
    }

    // ========== Helpers ==========

    private fun tms(
        employee: Employee,
        account: Account?,
        type: WorkingType,
        cat1: WorkingCategory1?,
        attended: Boolean,
    ): TeamMemberSchedule = TeamMemberSchedule(
        employee = employee,
        account = account,
        workingType = type,
        workingCategory1 = cat1,
    ).apply {
        if (attended) attendanceLog = AttendanceLog()
    }

    private fun displayMaster(
        employee: Employee,
        account: Account,
        typeOfWork5: TypeOfWork5 = TypeOfWork5.REGULAR,
    ): DisplayWorkSchedule =
        DisplayWorkSchedule(
            confirmed = true,
            typeOfWork1 = TypeOfWork1.DISPLAY,
            typeOfWork5 = typeOfWork5,
            typeOfWork3 = TypeOfWork3.FIXED,
        ).apply {
            this.employee = employee
            this.account = account
        }

    private fun safetySubmission(employeeId: Long): SafetyCheckSubmission =
        SafetyCheckSubmission(employeeId = employeeId, workingDate = LocalDate.of(2026, 6, 10))

    private fun createEmployee(id: Long, name: String): Employee = Employee(
        id = id,
        employeeCode = "20030$id",
        name = name,
        password = "encoded",
        role = AppAuthority.WOMAN,
        costCenterCode = "C001",
        status = "활동",
    )

    private fun createAccount(id: Long, name: String, externalKey: String? = null): Account = Account(
        id = id,
        name = name,
        branchCode = "C001",
        accountGroup = "1000",
        externalKey = externalKey,
    )
}
