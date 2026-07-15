package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * `AdminMonthlyIntegrationService.getIntegrationDetail` — MFEIS row 상세(집계 근거 일정) 단위 테스트.
 *
 * 검증 축: 집계 근거 일정을 FK(monthly_female_employee_integration_schedule_id) 역참조로 조회,
 * FK 미채움 시 externalKey 재매칭 폴백, 일별 1/N 기여분(분모 N 은 키 조합 외 일정 포함 — 월 모수 기준),
 * 근무일자 정렬, 요약 필드 매핑, not-found 예외.
 */
@DisplayName("AdminMonthlyIntegrationService.getIntegrationDetail — 집계 근거 일정 상세")
class AdminMonthlyIntegrationServiceDetailTest {

    private val organizationRepository: OrganizationRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk(relaxed = true)
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository =
        mockk(relaxed = true)
    private val branchCodeExpander: BranchCodeExpander = mockk(relaxed = true)
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository = mockk(relaxed = true)
    private val employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository = mockk(relaxed = true)
    private val teamMemberScheduleSearchService: TeamMemberScheduleSearchService = mockk(relaxed = true)
    private val teamMemberCategorySearchService: TeamMemberCategorySearchService = mockk(relaxed = true)

    private lateinit var service: AdminMonthlyIntegrationService

    private val yearMonth = YearMonth.of(2026, 6)
    private val employee = Employee(
        id = 10L, employeeCode = "00000010", name = "김여사", costCenterCode = "4889", orgName = "서울1지점"
    )

    @BeforeEach
    fun setUp() {
        service = AdminMonthlyIntegrationService(
            organizationRepository,
            employeeRepository,
            teamMemberScheduleRepository,
            accountRepository,
            monthlySalesHistoryGateway,
            monthlyIntegrationScheduleRepository,
            branchCodeExpander,
            accountCategoryMasterRepository,
            employeeInputCriteriaMasterRepository,
            teamMemberScheduleSearchService,
            teamMemberCategorySearchService,
        )
    }

    private fun stubPopulation(rows: List<TeamMemberSchedule>) {
        every {
            teamMemberScheduleRepository.findAttendedSchedulesByEmployeeAndMonth(
                10L, yearMonth.atDay(1), yearMonth.atEndOfMonth()
            )
        } returns rows
    }

    private fun account(id: Long, code: String? = "EXT$id"): Account =
        Account(id = id, name = "거래처$id", externalKey = code)

    private fun schedule(
        id: Long,
        account: Account?,
        date: LocalDate,
        attendedAt: LocalDateTime? = null,
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = employee,
        account = account,
        workingDate = date,
        workingType = WorkingType.WORK,
        workingCategory1 = WorkingCategory1.DISPLAY,
        workingCategory3 = WorkingCategory3.FIXED,
        costCenterCode = "4889",
        professionalPromotionTeam = "일반",
        attendanceLog = attendedAt?.let { AttendanceLog(attendanceDate = it) },
    )

    /** `schedule()` 헬퍼 기본 조합의 레거시 ExternalKey — 진열/고정 + 근무유형4/5 null 리터럴 + 일반. */
    private fun legacyKey(accountCode: String): String =
        "20266" + accountCode + "4889" + "00000010" + "진열" + "고정" + "null" + "null" + "일반"

    private fun mfeis(externalKey: String?, account: Account?): MonthlyFemaleEmployeeIntegrationSchedule =
        MonthlyFemaleEmployeeIntegrationSchedule(
            id = 77L,
            externalKey = externalKey,
            year = "2026",
            month = "6",
            employee = employee,
            account = account,
            costCenterCode = "4889",
            workingCategory1 = "진열",
            workingCategory3 = "고정",
            empBranchName = "서울1지점",
            workingDaysMonth = BigDecimal("2"),
            numberOfInputs = BigDecimal("2"),
            equivalentNumberOfWorkingDays = BigDecimal("1.5000"),
            convertedHeadcount = BigDecimal("0.7500"),
        )

    @Test
    @DisplayName("FK 미채움 폴백 — 집계 키 매칭 일정만 반환 + 일별 1/N 기여분 (분모 N 은 다른 키 조합 일정 포함)")
    fun sourceSchedulesFilteredByExternalKeyWithDailyContribution() {
        val accA = account(100L)
        val accB = account(200L)
        val day1 = LocalDate.of(2026, 6, 15)
        val day2 = LocalDate.of(2026, 6, 16)
        // 6/15 는 accA + accB 동시 출근 (N=2 → accA 기여 0.5), 6/16 은 accA 단독 (N=1 → 기여 1.0)
        stubPopulation(
            listOf(
                schedule(2L, accB, day1),
                schedule(1L, accA, day1, attendedAt = LocalDateTime.of(2026, 6, 15, 8, 30)),
                schedule(3L, accA, day2),
            )
        )
        every { monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(77L) } returns
            mfeis(legacyKey("EXT100"), accA)

        val detail = service.getIntegrationDetail(77L)

        // accB 일정은 다른 집계 키 — 근거 목록에서 제외, 단 6/15 의 분모 N=2 에는 포함
        assertThat(detail.schedules).hasSize(2)
        assertThat(detail.schedules.map { it.scheduleId }).containsExactly(1L, 3L)
        val first = detail.schedules[0]
        assertThat(first.workingDate).isEqualTo(day1)
        assertThat(first.dailyScheduleCount).isEqualTo(2)
        assertThat(first.equivalentContribution).isEqualByComparingTo(BigDecimal("0.5000"))
        assertThat(first.attendanceReportedAt).isEqualTo(LocalDateTime.of(2026, 6, 15, 8, 30))
        assertThat(first.accountCode).isEqualTo("EXT100")
        assertThat(first.workingCategory1).isEqualTo("진열")
        val second = detail.schedules[1]
        assertThat(second.dailyScheduleCount).isEqualTo(1)
        assertThat(second.equivalentContribution).isEqualByComparingTo(BigDecimal("1.0000"))
    }

    @Test
    @DisplayName("FK 채워진 경우 — FK 역참조로 근거 일정 조회 (externalKey 재매칭 아님)")
    fun sourceSchedulesResolvedByFk() {
        val accA = account(100L)
        val accB = account(200L)
        val day1 = LocalDate.of(2026, 6, 15)
        val day2 = LocalDate.of(2026, 6, 16)
        // 분모 N 산출용 월 모수 (accA 6/15·6/16, accB 6/15 동시)
        stubPopulation(
            listOf(
                schedule(2L, accB, day1),
                schedule(1L, accA, day1, attendedAt = LocalDateTime.of(2026, 6, 15, 8, 30)),
                schedule(3L, accA, day2),
            )
        )
        every { monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(77L) } returns
            mfeis(legacyKey("EXT100"), accA)
        // FK 로 연결된 근거 일정 — accA 2건만 (accB 는 다른 MFEIS 소속). externalKey 재매칭과 무관하게 이 결과 사용.
        every { teamMemberScheduleRepository.findSchedulesByIntegrationScheduleId(77L) } returns
            listOf(
                schedule(1L, accA, day1, attendedAt = LocalDateTime.of(2026, 6, 15, 8, 30)),
                schedule(3L, accA, day2),
            )

        val detail = service.getIntegrationDetail(77L)

        assertThat(detail.schedules.map { it.scheduleId }).containsExactly(1L, 3L)
        // 분모 N 은 여전히 월 모수 기준 — 6/15 는 accA+accB 로 N=2
        assertThat(detail.schedules[0].dailyScheduleCount).isEqualTo(2)
        assertThat(detail.schedules[0].equivalentContribution).isEqualByComparingTo(BigDecimal("0.5000"))
        assertThat(detail.schedules[1].dailyScheduleCount).isEqualTo(1)
        assertThat(detail.schedules[1].equivalentContribution).isEqualByComparingTo(BigDecimal("1.0000"))
    }

    @Test
    @DisplayName("요약 필드 — MFEIS persist 값 그대로 매핑 (재계산 아님)")
    fun summaryFieldsFromPersistedRow() {
        val accA = account(100L)
        stubPopulation(emptyList())
        every { monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(77L) } returns
            mfeis(legacyKey("EXT100"), accA)

        val detail = service.getIntegrationDetail(77L)

        assertThat(detail.id).isEqualTo(77L)
        assertThat(detail.year).isEqualTo(2026)
        assertThat(detail.month).isEqualTo(6)
        assertThat(detail.branchName).isEqualTo("서울1지점")
        assertThat(detail.employeeCode).isEqualTo("00000010")
        assertThat(detail.employeeName).isEqualTo("김여사")
        assertThat(detail.accountCode).isEqualTo("EXT100")
        assertThat(detail.workingDaysMonth).isEqualTo(2)
        assertThat(detail.totalInputCount).isEqualTo(2)
        assertThat(detail.equivalentWorkingDays).isEqualByComparingTo(BigDecimal("1.5000"))
        assertThat(detail.convertedHeadcount).isEqualByComparingTo(BigDecimal("0.7500"))
        assertThat(detail.schedules).isEmpty()
    }

    @Test
    @DisplayName("employee 미연결 row — 근거 일정 없이 요약만 반환")
    fun missingEmployeeReturnsEmptySchedules() {
        val row = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 77L, externalKey = "any", year = "2026", month = "6",
            equivalentNumberOfWorkingDays = BigDecimal("1.0000"),
        )
        every { monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(77L) } returns row

        val detail = service.getIntegrationDetail(77L)

        assertThat(detail.schedules).isEmpty()
        assertThat(detail.equivalentWorkingDays).isEqualByComparingTo(BigDecimal("1.0000"))
    }

    @Test
    @DisplayName("존재하지 않는 id — MonthlyIntegrationNotFoundException")
    fun notFoundThrows() {
        every { monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(99L) } returns null

        assertThatThrownBy { service.getIntegrationDetail(99L) }
            .isInstanceOf(MonthlyIntegrationNotFoundException::class.java)
    }
}
