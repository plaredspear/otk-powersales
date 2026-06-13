package com.otoki.powersales.admin.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sales.service.MonthlySalesAdminQueryService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("AdminDashboardService 테스트 (실집계)")
class AdminDashboardServiceTest {

    private val mfeisRepository = mockk<MonthlyFemaleEmployeeIntegrationScheduleRepository>()
    private val employeeRepository = mockk<EmployeeRepository>()
    private val monthlySalesAdminQueryService = mockk<MonthlySalesAdminQueryService>()

    private val service = AdminDashboardService(
        mfeisRepository, employeeRepository, monthlySalesAdminQueryService,
    )

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    // -- fixtures --

    private fun account(id: Long, type: AccountType): Account =
        Account(id = id, accountType = type, externalKey = "SAP$id")

    private fun mfeis(
        accountType: AccountType? = AccountType.SUPER,
        wc1: String? = "진열",
        wc3: String? = "고정",
        headcount: BigDecimal = BigDecimal.ONE,
        acc: Account? = null,
    ): MonthlyFemaleEmployeeIntegrationSchedule =
        MonthlyFemaleEmployeeIntegrationSchedule(
            workingCategory1 = wc1,
            workingCategory3 = wc3,
            convertedHeadcount = headcount,
            account = acc ?: accountType?.let { account(1, it) },
        )

    private var empSeq = 0
    private fun employee(
        status: String? = "재직",
        jobCode: String? = null,
        birthDate: String? = null,
    ): Employee = Employee(
        employeeCode = "E${empSeq++}",
        name = "사원$empSeq",
        status = status,
        jobCode = jobCode,
        birthDate = birthDate,
    )

    private fun stubEmpty() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findAll() } returns emptyList()
        every { employeeRepository.findByCostCenterCodeIn(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)
    }

    @Test
    @DisplayName("T1 환산인원 SUM (거래처유형별) — 슈퍼 100.5+50.0 / 농협 30.0, scale=4")
    fun sumByAccountType() {
        val superAcc = account(1, AccountType.SUPER)
        val nhAcc = account(2, AccountType.NONGHYUP)
        val rows = listOf(
            mfeis(headcount = BigDecimal("100.5"), acc = superAcc),
            mfeis(headcount = BigDecimal("50.0"), acc = superAcc),
            mfeis(headcount = BigDecimal("30.0"), acc = nhAcc),
        )
        // 거래처유형별 차트는 전월(마감) 기준 → previousYm rows 로 반환
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findAll() } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)
        val byType = result.staffDeployment.byAccountType.associateBy { it.accountType }

        assertThat(byType[AccountType.SUPER.displayName]!!.convertedHeadcount)
            .isEqualByComparingTo(BigDecimal("150.5000"))
        assertThat(byType[AccountType.SUPER.displayName]!!.convertedHeadcount.scale()).isEqualTo(4)
        assertThat(byType[AccountType.NONGHYUP.displayName]!!.convertedHeadcount)
            .isEqualByComparingTo(BigDecimal("30.0000"))
    }

    @Test
    @DisplayName("T2 근무유형1 비중 — 진열 960 / 행사 510")
    fun sumByWorkType() {
        val rows = listOf(
            mfeis(wc1 = "진열", headcount = BigDecimal("960")),
            mfeis(wc1 = "행사", headcount = BigDecimal("510")),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns emptyList()
        every { employeeRepository.findAll() } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)
        val byWork = result.staffDeployment.byWorkType.associateBy { it.workType }

        assertThat(byWork["진열"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("960.0000"))
        assertThat(byWork["행사"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("510.0000"))
    }

    @Test
    @DisplayName("T3 유통×근무형태 — 슈퍼 고정 400 / 순회 54.9")
    fun sumByChannelAndWorkType() {
        val superAcc = account(1, AccountType.SUPER)
        val rows = listOf(
            mfeis(wc3 = "고정", headcount = BigDecimal("400"), acc = superAcc),
            mfeis(wc3 = "순회", headcount = BigDecimal("54.9"), acc = superAcc),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns emptyList()
        every { employeeRepository.findAll() } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)
        val superItem = result.staffDeployment.byChannelAndWorkType
            .first { it.channelName == AccountType.SUPER.displayName }

        assertThat(superItem.fixedHeadcount).isEqualByComparingTo(BigDecimal("400.0000"))
        assertThat(superItem.alternatingHeadcount).isEqualByComparingTo(BigDecimal("0.0000"))
        assertThat(superItem.visitingHeadcount).isEqualByComparingTo(BigDecimal("54.9000"))
        assertThat(superItem.fixed).isEqualTo(1)
        assertThat(superItem.visiting).isEqualTo(1)
    }

    @Test
    @DisplayName("T4/T6 매출 실적 + 전년 대비 — actual 800, lastYear 760 -> ratio ≈ 105.3")
    fun salesActualAndLastYearRatio() {
        val acc = account(1, AccountType.SUPER)
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns
            listOf(mfeis(acc = acc))
        every { employeeRepository.findAll() } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(actualAmount = 800L, lastYearAmount = 760L)

        val result = service.getDashboard(allScope, "2026-05", null)

        assertThat(result.salesSummary.actualAmount).isEqualTo(800L)
        assertThat(result.salesSummary.lastYearAmount).isEqualTo(760L)
        assertThat(result.salesSummary.lastYearRatio).isCloseTo(105.26, org.assertj.core.data.Offset.offset(0.1))
        // D7 — 목표/진도율/채널 후속
        assertThat(result.salesSummary.targetAmount).isZero()
        assertThat(result.salesSummary.progressRate).isZero()
        assertThat(result.salesSummary.channelSales).isEmpty()
    }

    @Test
    @DisplayName("D4 기준진도율 — 미래월 0.0 / 과거월 100.0 / 당월 달력일 비율")
    fun calendarReferenceProgressRate() {
        val today = LocalDate.of(2026, 5, 15)
        // 당월 5월: 15/31
        assertThat(service.calendarReferenceProgressRate(YearMonth.of(2026, 5), today))
            .isCloseTo(15.0 / 31.0 * 100.0, org.assertj.core.data.Offset.offset(0.01))
        // 과거월 4월
        assertThat(service.calendarReferenceProgressRate(YearMonth.of(2026, 4), today)).isEqualTo(100.0)
        // 미래월 6월
        assertThat(service.calendarReferenceProgressRate(YearMonth.of(2026, 6), today)).isEqualTo(0.0)
    }

    @Test
    @DisplayName("T7 연령 버킷팅 — birthDate 1995-03-01 기준 2026-05 만 31세 -> 30대")
    fun ageGroupBucketing() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findAll() } returns listOf(employee(birthDate = "1995-03-01"))
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)
        val byAge = result.basicStats.byAgeGroup.associateBy { it.ageGroup }

        assertThat(byAge["30대"]!!.count).isEqualTo(1)
    }

    @Test
    @DisplayName("T8 연령 birthDate null -> 미상 버킷")
    fun ageGroupUnknown() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findAll() } returns listOf(employee(birthDate = null))
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)

        assertThat(result.basicStats.byAgeGroup.first { it.ageGroup == "미상" }.count).isEqualTo(1)
    }

    @Test
    @DisplayName("T9 재직/휴직 분류 — 재직 3 / 휴직 1")
    fun activeOnLeave() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findAll() } returns listOf(
            employee(status = "재직"), employee(status = "재직"), employee(status = "재직"),
            employee(status = "휴직"),
        )
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)

        assertThat(result.basicStats.totalByPosition.active).isEqualTo(3)
        assertThat(result.basicStats.totalByPosition.onLeave).isEqualTo(1)
    }

    @Test
    @DisplayName("D6 판촉/OSC — 판촉직 2 / OSC직·레이디직 합산 2")
    fun promotionOscByJobCode() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findAll() } returns listOf(
            employee(jobCode = "판촉직"), employee(jobCode = "판촉직"),
            employee(jobCode = "OSC직"), employee(jobCode = "레이디직"),
            employee(jobCode = "기타"),
        )
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(0L, 0L)

        val result = service.getDashboard(allScope, "2026-05", null)

        assertThat(result.basicStats.staffType.promotion).isEqualTo(2)
        assertThat(result.basicStats.staffType.osc).isEqualTo(2)
    }

    @Test
    @DisplayName("T10 빈 데이터 — 3섹션 전부 0 / 빈 리스트 정상 반환")
    fun emptyData() {
        stubEmpty()

        val result = service.getDashboard(allScope, "2026-05", null)

        assertThat(result.salesSummary.actualAmount).isZero()
        assertThat(result.staffDeployment.byAccountType).isEmpty()
        assertThat(result.staffDeployment.byWorkType).isEmpty()
        assertThat(result.basicStats.staffType.promotion).isZero()
        assertThat(result.basicStats.byAgeGroup).isEmpty()
    }

    @Test
    @DisplayName("T11 yearMonth 미지정 시 당월 사용")
    fun defaultsToCurrentMonth() {
        stubEmpty()
        val expected = YearMonth.now().toString()

        val result = service.getDashboard(allScope, null, null)

        assertThat(result.salesSummary.yearMonth).isEqualTo(expected)
        assertThat(result.staffDeployment.yearMonth).isEqualTo(expected)
    }
}
