package com.otoki.powersales.domain.activity.schedule.service

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
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * `AdminMonthlyIntegrationService.refreshIntegration` — SF 레거시
 * `TeamMemberScheduleTriggerHandler.updateMonthlyFemaleEmployeeIntegrationSchedule` (insert/update 경로)
 * 정합 단위 테스트.
 *
 * 검증 축: 레거시 ExternalKey 포맷(무패딩 월 + null 리터럴), 환산근무일수 1/N 전정밀도 누적(선반올림 금지),
 * 당월근무일수(사원+costCenter distinct 날짜), 총투입횟수(거래처+유형 조합 distinct 날짜),
 * 기존 row ExternalKey upsert(EmpBranchName 유지), stale 키 삭제, 모수 0건 전체 삭제.
 */
@DisplayName("AdminMonthlyIntegrationService.refreshIntegration — SF 레거시 집계 정합")
class AdminMonthlyIntegrationServiceRefreshTest {

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
        every { monthlyIntegrationScheduleRepository.save(any()) } answers { firstArg() }
        every { monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6") } returns emptyList()
        every {
            monthlyIntegrationScheduleRepository.findByAccountIdAndWorkingCategory1AndYearAndMonth(any(), any(), any(), any())
        } returns emptyList()
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
        cat1: WorkingCategory1? = WorkingCategory1.DISPLAY,
        cat3: WorkingCategory3? = WorkingCategory3.FIXED,
        costCenterCode: String? = "4889",
        professionalPromotionTeam: String? = "일반",
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = employee,
        account = account,
        workingDate = date,
        workingType = WorkingType.WORK,
        workingCategory1 = cat1,
        workingCategory3 = cat3,
        costCenterCode = costCenterCode,
        professionalPromotionTeam = professionalPromotionTeam,
    )

    private fun savedRecords(): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val records = mutableListOf<MonthlyFemaleEmployeeIntegrationSchedule>()
        verify { monthlyIntegrationScheduleRepository.save(capture(records)) }
        return records
    }

    @Test
    @DisplayName("ExternalKey — 레거시 getExternalKey 포맷 (무패딩 월 + null 리터럴 + 컴포넌트 순서)")
    fun legacyExternalKeyFormat() {
        val acc = account(100L, "1234567")
        stubPopulation(listOf(schedule(1L, acc, LocalDate.of(2026, 6, 15))))

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords().single()
        // 년(2026) + 월(6, 무패딩) + 거래처코드 + costCenter + 사번 + 진열 + 고정 + null(근무유형4)
        // + null(근무유형5) + 일반(전문판촉팀)
        assertThat(saved.externalKey).isEqualTo("2026612345674889" + "00000010" + "진열" + "고정" + "null" + "null" + "일반")
        assertThat(saved.year).isEqualTo("2026")
        assertThat(saved.month).isEqualTo("6")
        assertThat(saved.costCenterCode).isEqualTo("4889")
        assertThat(saved.workingCategory1).isEqualTo("진열")
        assertThat(saved.workingCategory3).isEqualTo("고정")
        assertThat(saved.empBranchName).isEqualTo("서울1지점")
        // 미배정('일반') TMS 값을 무변환 저장 — enum 변환 없이 String 그대로 (SF 마이그레이션 row 와 동일 값).
        // 과거 enum 변환(fromDisplayNameOrNull) 은 '일반' 을 null 로 떨어뜨려 재집계 row 만 null 이 되는 회귀가 있었다.
        assertThat(saved.professionalPromotionTeam).isEqualTo("일반")
    }

    @Test
    @DisplayName("전문행사조 — 정식 조 배정 사원은 TMS 값(조 이름)을 그대로 MFEIS 에 저장")
    fun professionalPromotionTeamAssigned() {
        val acc = account(100L, "1234567")
        stubPopulation(
            listOf(schedule(1L, acc, LocalDate.of(2026, 6, 15), professionalPromotionTeam = "프레시세일조_냉동"))
        )

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords().single()
        assertThat(saved.professionalPromotionTeam).isEqualTo("프레시세일조_냉동")
    }

    @Test
    @DisplayName("환산근무일수 — 1/N 전정밀도 누적 후 최종 4자리 반올림 (1/3 × 3일 = 1.0000, 0.9999 아님)")
    fun equivalentWorkingDaysFullPrecision() {
        val accA = account(100L)
        val accB = account(200L)
        val accC = account(300L)
        // 3일간 매일 3개 거래처 동시 출근 — 거래처별 일 기여 1/3, 3일 누적 = 1.0
        val rows = (15..17).flatMap { day ->
            val date = LocalDate.of(2026, 6, day)
            listOf(
                schedule(day * 10L + 1, accA, date),
                schedule(day * 10L + 2, accB, date),
                schedule(day * 10L + 3, accC, date),
            )
        }
        stubPopulation(rows)

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords()
        assertThat(saved).hasSize(3)
        saved.forEach {
            // 선반올림(0.3333×3=0.9999) 이 아니라 전정밀도 누적 후 반올림 = 1.0000 (레거시 Double 합산 동등)
            assertThat(it.equivalentNumberOfWorkingDays).isEqualByComparingTo(BigDecimal("1.0000"))
            // 당월근무일수 = 사원+costCenter distinct 날짜 3 / 환산인원 = 1.0 / 3
            assertThat(it.workingDaysMonth).isEqualByComparingTo(BigDecimal("3"))
            assertThat(it.convertedHeadcount).isEqualByComparingTo(BigDecimal("0.3333"))
        }
    }

    @Test
    @DisplayName("환산인원 사원 합계 — 날짜별 N 이 제각각인 비대칭 케이스도 SF 동형 Double 계산으로 정확히 1.0000 (0.9999/1.0001 오차 없음)")
    fun convertedHeadcountEmployeeSumIsExactlyOne() {
        val accA = account(100L)
        val accB = account(200L)
        val accC = account(300L)
        // D = 3일 근무. 날짜별 그날 투입 거래처 수 N 이 3 → 2 → 1 로 제각각인 비대칭 구조.
        //   6/15: A,B,C (N=3) / 6/16: A,B (N=2) / 6/17: A (N=1)
        // SF 는 1/N 을 Double 로 반올림 없이 누적 후 /D, 저장 직전 4자리 반올림 1회.
        //   A행 = (1/3+1/2+1/1)/3 = 0.6111, B행 = (1/3+1/2)/3 = 0.2778, C행 = (1/3)/3 = 0.1111
        //   사원 합계 = 0.6111+0.2778+0.1111 = 1.0000 (날짜별 몫 합 = 1 의 D 배 / D)
        // BigDecimal 로 행마다 divide(4,HALF_UP) 선반올림하면 이 합이 0.9999/1.0001 로 어긋난다.
        stubPopulation(
            listOf(
                schedule(1L, accA, LocalDate.of(2026, 6, 15)),
                schedule(2L, accB, LocalDate.of(2026, 6, 15)),
                schedule(3L, accC, LocalDate.of(2026, 6, 15)),
                schedule(4L, accA, LocalDate.of(2026, 6, 16)),
                schedule(5L, accB, LocalDate.of(2026, 6, 16)),
                schedule(6L, accA, LocalDate.of(2026, 6, 17)),
            )
        )

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords()
        assertThat(saved).hasSize(3)
        assertThat(saved).allSatisfy {
            assertThat(it.workingDaysMonth).isEqualByComparingTo(BigDecimal("3"))
        }
        val sum = saved.mapNotNull { it.convertedHeadcount }
            .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        // 저장값(4자리) 단순 합산이 정확히 1.0000 — 이미지의 알라딘 실측치 정합
        assertThat(sum).isEqualByComparingTo(BigDecimal("1.0000"))
    }

    @Test
    @DisplayName("총투입횟수 — 거래처+유형 조합의 distinct 날짜 (같은 날 같은 조합 중복 row 는 1회)")
    fun numberOfInputsDistinctDates() {
        val acc = account(100L)
        val date = LocalDate.of(2026, 6, 15)
        stubPopulation(
            listOf(
                schedule(1L, acc, date),
                schedule(2L, acc, date), // 같은 날 같은 조합 중복 row
                schedule(3L, acc, LocalDate.of(2026, 6, 16)),
            )
        )

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords().single()
        // distinct 날짜 = 2 (레거시 countSet 은 Set 이라 같은 날 중복이 1개로 수렴)
        assertThat(saved.numberOfInputs).isEqualByComparingTo(BigDecimal("2"))
        // 환산근무일수 = 15일 (1/2 + 1/2) + 16일 (1/1) = 2.0
        assertThat(saved.equivalentNumberOfWorkingDays).isEqualByComparingTo(BigDecimal("2.0000"))
    }

    @Test
    @DisplayName("집계 키 분리 — 같은 사원×거래처×월이라도 근무유형3 이 다르면 별도 MFEIS row")
    fun splitByWorkingCategory() {
        val acc = account(100L)
        stubPopulation(
            listOf(
                schedule(1L, acc, LocalDate.of(2026, 6, 15), cat3 = WorkingCategory3.FIXED),
                schedule(2L, acc, LocalDate.of(2026, 6, 16), cat3 = WorkingCategory3.PATROL),
            )
        )

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords()
        assertThat(saved).hasSize(2)
        assertThat(saved.map { it.workingCategory3 }).containsExactlyInAnyOrder("고정", "순회")
        saved.forEach { assertThat(it.equivalentNumberOfWorkingDays).isEqualByComparingTo(BigDecimal("1.0000")) }
    }

    @Test
    @DisplayName("기존 row upsert — ExternalKey 일치 시 집계 필드만 갱신 + 비공백 EmpBranchName 유지 (레거시 250409)")
    fun upsertPreservesEmpBranchName() {
        val acc = account(100L, "1234567")
        val key = "2026612345674889" + "00000010" + "진열" + "고정" + "null" + "null" + "일반"
        val existing = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 77L, externalKey = key, year = "2026", month = "6",
            employee = employee, account = acc,
            empBranchName = "부산1지점", // 지점 이동 전 이력 값 — 유지 대상
            equivalentNumberOfWorkingDays = BigDecimal("9.9999"),
        )
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6")
        } returns listOf(existing)
        stubPopulation(listOf(schedule(1L, acc, LocalDate.of(2026, 6, 15))))

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords().single()
        assertThat(saved.id).isEqualTo(77L) // 새 row 생성이 아니라 기존 row 갱신
        assertThat(saved.empBranchName).isEqualTo("부산1지점")
        assertThat(saved.equivalentNumberOfWorkingDays).isEqualByComparingTo(BigDecimal("1.0000"))
        verify(exactly = 0) { monthlyIntegrationScheduleRepository.delete(any()) }
    }

    @Test
    @DisplayName("stale 삭제 — 재집계 후 키 조합이 사라진 기존 row 는 삭제 (레거시 deleteRecordsSet 동등)")
    fun deletesStaleRows() {
        val acc = account(100L, "1234567")
        val stale = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 88L, externalKey = "20266" + "OLDKEY", year = "2026", month = "6", employee = employee,
        )
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6")
        } returns listOf(stale)
        stubPopulation(listOf(schedule(1L, acc, LocalDate.of(2026, 6, 15))))

        service.refreshIntegration(10L, yearMonth)

        val deleted = slot<List<MonthlyFemaleEmployeeIntegrationSchedule>>()
        verify { monthlyIntegrationScheduleRepository.deleteAll(capture(deleted)) }
        assertThat(deleted.captured.map { it.id }).containsExactly(88L)
    }

    @Test
    @DisplayName("모수 0건 — 사원×월 기존 MFEIS row 전건 삭제 후 종료")
    fun emptyPopulationDeletesAll() {
        val existing = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 99L, externalKey = "any", year = "2026", month = "6", employee = employee,
        )
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6")
        } returns listOf(existing)
        stubPopulation(emptyList())

        service.refreshIntegration(10L, yearMonth)

        verify { monthlyIntegrationScheduleRepository.deleteAll(listOf(existing)) }
        verify(exactly = 0) { monthlyIntegrationScheduleRepository.save(any()) }
    }

    @Test
    @DisplayName("집계 근거 FK 세팅 — 각 근거 TMS row 를 해당 MFEIS row 로 연결 (상세 FK 역참조용)")
    fun setsIntegrationFkOnSourceSchedules() {
        val accA = account(100L)
        val accB = account(200L)
        val date = LocalDate.of(2026, 6, 15)
        // accA / accB 는 서로 다른 집계 키 → 각각 별도 MFEIS row
        val rowA = schedule(1L, accA, date)
        val rowB = schedule(2L, accB, date)
        stubPopulation(listOf(rowA, rowB))

        service.refreshIntegration(10L, yearMonth)

        // 각 근거 row 는 자신의 집계 키에 해당하는 MFEIS 로 FK 연결 (영속 entity dirty checking — 명시 save 아님)
        assertThat(rowA.monthlyFemaleEmployeeIntegrationSchedule?.account?.id).isEqualTo(100L)
        assertThat(rowB.monthlyFemaleEmployeeIntegrationSchedule?.account?.id).isEqualTo(200L)
    }

    @Test
    @DisplayName("stale 삭제 전 FK detach — 삭제되는 MFEIS id 로 TMS FK 를 벌크 null 처리 후 삭제")
    fun detachesFkBeforeStaleDelete() {
        val acc = account(100L, "1234567")
        val stale = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 88L, externalKey = "20266" + "OLDKEY", year = "2026", month = "6", employee = employee,
        )
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6")
        } returns listOf(stale)
        stubPopulation(listOf(schedule(1L, acc, LocalDate.of(2026, 6, 15))))

        service.refreshIntegration(10L, yearMonth)

        // 삭제 대상 stale MFEIS id 로 벌크 detach 호출 (dangling FK 제거)
        val detachIds = slot<List<Long>>()
        verify { teamMemberScheduleRepository.detachIntegrationScheduleByIds(capture(detachIds)) }
        assertThat(detachIds.captured).containsExactly(88L)
        val deleted = slot<List<MonthlyFemaleEmployeeIntegrationSchedule>>()
        verify { monthlyIntegrationScheduleRepository.deleteAll(capture(deleted)) }
        assertThat(deleted.captured.map { it.id }).containsExactly(88L)
    }

    @Test
    @DisplayName("모수 0건 — FK 벌크 detach 후 사원×월 기존 MFEIS row 전건 삭제")
    fun emptyPopulationDetachesFkThenDeletesAll() {
        val existing = MonthlyFemaleEmployeeIntegrationSchedule(
            id = 99L, externalKey = "any", year = "2026", month = "6", employee = employee,
        )
        every {
            monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(10L, "2026", "6")
        } returns listOf(existing)
        stubPopulation(emptyList())

        service.refreshIntegration(10L, yearMonth)

        val detachIds = slot<List<Long>>()
        verify { teamMemberScheduleRepository.detachIntegrationScheduleByIds(capture(detachIds)) }
        assertThat(detachIds.captured).containsExactly(99L)
        verify { monthlyIntegrationScheduleRepository.deleteAll(listOf(existing)) }
    }

    @Test
    @DisplayName("당월근무일수 — costCenter 가 다른 날은 해당 키 분모에서 제외 (사원+costCenter 단위)")
    fun workingDaysMonthPerCostCenter() {
        val acc = account(100L)
        stubPopulation(
            listOf(
                schedule(1L, acc, LocalDate.of(2026, 6, 15), costCenterCode = "4889"),
                schedule(2L, acc, LocalDate.of(2026, 6, 16), costCenterCode = "4889"),
                schedule(3L, acc, LocalDate.of(2026, 6, 17), costCenterCode = "5000"), // 지점 이동 후
            )
        )

        service.refreshIntegration(10L, yearMonth)

        val saved = savedRecords()
        assertThat(saved).hasSize(2) // costCenter 가 키 컴포넌트 → row 분리
        val at4889 = saved.single { it.costCenterCode == "4889" }
        val at5000 = saved.single { it.costCenterCode == "5000" }
        assertThat(at4889.workingDaysMonth).isEqualByComparingTo(BigDecimal("2"))
        assertThat(at5000.workingDaysMonth).isEqualByComparingTo(BigDecimal("1"))
    }
}
