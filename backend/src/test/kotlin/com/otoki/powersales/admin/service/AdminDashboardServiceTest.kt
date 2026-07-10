package com.otoki.powersales.admin.service

import com.otoki.powersales.domain.org.employee.repository.DashboardEmployeeProjection
import com.otoki.powersales.domain.activity.schedule.repository.DashboardDeploymentRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesAdminQueryService
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

    // -- fixtures --

    /** 투입 거래처 식별 — (id, accountType) 쌍. externalKey 는 "SAP{id}" 규칙. */
    private fun account(id: Long, type: String): Pair<Long, String> = id to type

    private fun mfeis(
        accountType: String? = "슈퍼",
        wc1: String? = "진열",
        wc3: String? = "고정",
        headcount: BigDecimal = BigDecimal.ONE,
        acc: Pair<Long, String>? = null,
    ): DashboardDeploymentRow {
        val resolved = acc ?: accountType?.let { account(1, it) }
        return DashboardDeploymentRow(
            convertedHeadcount = headcount,
            workingCategory1 = wc1,
            workingCategory3 = wc3,
            accountId = resolved?.first,
            accountExternalKey = resolved?.let { "SAP${it.first}" },
            accountType = resolved?.second,
        )
    }

    private var empSeq = 0
    private fun employee(
        status: String? = "재직",
        jobCode: String? = null,
        birthDate: String? = null,
    ): DashboardEmployeeProjection {
        empSeq++
        val s = status; val j = jobCode; val b = birthDate
        return object : DashboardEmployeeProjection {
            override val status = s
            override val jobCode = j
            override val birthDate = b
        }
    }

    private fun stubEmpty() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )
    }

    @Test
    @DisplayName("T1 환산인원 SUM (거래처유형별) — 슈퍼 100.5+50.0 / 농협 30.0, scale=4")
    fun sumByAccountType() {
        val superAcc = account(1, "슈퍼")
        val nhAcc = account(2, "농협")
        val rows = listOf(
            mfeis(headcount = BigDecimal("100.5"), acc = superAcc),
            mfeis(headcount = BigDecimal("50.0"), acc = superAcc),
            mfeis(headcount = BigDecimal("30.0"), acc = nhAcc),
        )
        // 거래처유형별 차트는 전월(마감) 기준 → previousYm rows 로 반환
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")
        val byType = result.staffDeployment.byAccountType.associateBy { it.accountType }

        assertThat(byType["슈퍼"]!!.convertedHeadcount)
            .isEqualByComparingTo(BigDecimal("150.5000"))
        assertThat(byType["슈퍼"]!!.convertedHeadcount.scale()).isEqualTo(4)
        assertThat(byType["농협"]!!.convertedHeadcount)
            .isEqualByComparingTo(BigDecimal("30.0000"))
    }

    @Test
    @DisplayName("T2 근무유형1 비중 — 진열 960 / 행사 510 (전월 마감 기준)")
    fun sumByWorkType() {
        val rows = listOf(
            mfeis(wc1 = "진열", headcount = BigDecimal("960")),
            mfeis(wc1 = "행사", headcount = BigDecimal("510")),
        )
        // 투입현황 전 차트는 전월(마감) 기준 → previousYm rows 로 반환
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")
        val byWork = result.staffDeployment.byWorkType.associateBy { it.workType }

        assertThat(byWork["진열"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("960.0000"))
        assertThat(byWork["행사"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("510.0000"))
    }

    @Test
    @DisplayName("T3 유통×근무형태 — 슈퍼 고정 400 / 순회 54.9 (전월 마감 기준)")
    fun sumByChannelAndWorkType() {
        val superAcc = account(1, "슈퍼")
        val rows = listOf(
            mfeis(wc3 = "고정", headcount = BigDecimal("400"), acc = superAcc),
            mfeis(wc3 = "순회", headcount = BigDecimal("54.9"), acc = superAcc),
        )
        // 투입현황 전 차트는 전월(마감) 기준 → previousYm rows 로 반환
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")
        val superItem = result.staffDeployment.byChannelAndWorkType
            .first { it.channelName == "슈퍼" }

        assertThat(superItem.fixedHeadcount).isEqualByComparingTo(BigDecimal("400.0000"))
        assertThat(superItem.alternatingHeadcount).isEqualByComparingTo(BigDecimal("0.0000"))
        assertThat(superItem.visitingHeadcount).isEqualByComparingTo(BigDecimal("54.9000"))
        assertThat(superItem.fixed).isEqualTo(1)
        assertThat(superItem.visiting).isEqualTo(1)
    }

    @Test
    @DisplayName("T3-1 기본현황 근무형태별(고정/격고/순회)은 환산인원 SUM — 고정 400+1=401 / 순회 54.9, scale=4")
    fun basicStatsByWorkTypeUsesConvertedHeadcount() {
        val rows = listOf(
            mfeis(wc3 = "고정", headcount = BigDecimal("400")),
            mfeis(wc3 = "고정", headcount = BigDecimal("1")),
            mfeis(wc3 = "순회", headcount = BigDecimal("54.9")),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")
        val byWorkType = result.basicStats.byWorkType

        assertThat(byWorkType.fixed).isEqualByComparingTo(BigDecimal("401.0000"))
        assertThat(byWorkType.alternating).isEqualByComparingTo(BigDecimal("0.0000"))
        assertThat(byWorkType.visiting).isEqualByComparingTo(BigDecimal("54.9000"))
    }

    @Test
    @DisplayName("T4/T6 매출 실적 + 전년 대비 — actual 800, lastYear 760 -> ratio ≈ 105.3")
    fun salesActualAndLastYearRatio() {
        val acc = account(1, "슈퍼")
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns
            listOf(mfeis(acc = acc))
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 800L, targetAmount = 1000L, lastYearAmount = 760L,
                hasActualData = true, hasLastYearData = true, hasTargetData = true,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        assertThat(result.salesSummary.actualAmount).isEqualTo(800L)
        assertThat(result.salesSummary.lastYearAmount).isEqualTo(760L)
        assertThat(result.salesSummary.lastYearRatio).isCloseTo(105.26, org.assertj.core.data.Offset.offset(0.1))
        // 목표 + 달성률 — round(800 / 1000 × 100) = 80.0
        assertThat(result.salesSummary.targetAmount).isEqualTo(1000L)
        assertThat(result.salesSummary.progressRate).isEqualTo(80.0)
        // 데이터 적재 여부 플래그 전달
        assertThat(result.salesSummary.hasActualData).isTrue()
        assertThat(result.salesSummary.hasLastYearData).isTrue()
        assertThat(result.salesSummary.hasTargetData).isTrue()
        // 유통별 목표/진도율(channelSales)은 데이터 부재로 빈 리스트
        assertThat(result.salesSummary.channelSales).isEmpty()
    }

    @Test
    @DisplayName("T4-2 당월 목표 미등록 — targetAmount 0 + hasTargetData false + progressRate 0 (계산은 목표 0)")
    fun salesTargetNotRegistered() {
        val acc = account(1, "슈퍼")
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns
            listOf(mfeis(acc = acc))
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        // 목표 미등록 — 실적은 있으나 목표 row 전무 (hasTargetData=false, target=0)
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 800L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = true, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        // 화면 "—" 신호 — 목표 미등록
        assertThat(result.salesSummary.hasTargetData).isFalse()
        // 계산은 목표 0 으로 — 달성률 0.0 (NaN/Infinity 없이)
        assertThat(result.salesSummary.targetAmount).isEqualTo(0L)
        assertThat(result.salesSummary.progressRate).isEqualTo(0.0)
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
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(employee(birthDate = "1995-03-01"))
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")
        val byAge = result.basicStats.byAgeGroup.associateBy { it.ageGroup }

        assertThat(byAge["30대"]!!.count).isEqualTo(1)
    }

    @Test
    @DisplayName("T8 연령 birthDate null -> 미상 버킷")
    fun ageGroupUnknown() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(employee(birthDate = null))
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        assertThat(result.basicStats.byAgeGroup.first { it.ageGroup == "미상" }.count).isEqualTo(1)
    }

    @Test
    @DisplayName("T9 재직/휴직 분류 — 재직 3 / 휴직 1 / 기타(null) 1. 퇴직자는 repository 에서 제외되어 모수 미포함")
    fun activeOnLeave() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        // 퇴직자는 findDashboardBasicStatsProjection 쿼리 레벨에서 제외되므로 mock 입력에도 포함하지 않는다.
        // etc 잔차에는 status=null 만 남는다.
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(status = "재직"), employee(status = "재직"), employee(status = "재직"),
            employee(status = "휴직"),
            employee(status = null),
        )
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        assertThat(result.basicStats.totalByPosition.active).isEqualTo(3)
        assertThat(result.basicStats.totalByPosition.onLeave).isEqualTo(1)
        assertThat(result.basicStats.totalByPosition.etc).isEqualTo(1)
        // 기타(null) → "미분류" 1명 breakdown
        assertThat(result.basicStats.totalByPosition.etcBreakdown)
            .extracting("label", "count")
            .containsExactly(org.assertj.core.groups.Tuple.tuple("미분류", 1))
    }

    @Test
    @DisplayName("D6 판촉/OSC — 판촉직 2 / OSC직·레이디직 합산 2")
    fun promotionOscByJobCode() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직"), employee(jobCode = "판촉직"),
            employee(jobCode = "OSC직"), employee(jobCode = "레이디직"),
            employee(jobCode = "기타"),
        )
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        assertThat(result.basicStats.staffType.promotion).isEqualTo(2)
        assertThat(result.basicStats.staffType.osc).isEqualTo(2)
        assertThat(result.basicStats.staffType.etc).isEqualTo(1)
        // 기타(jobCode="기타") → "기타" 1명 breakdown
        assertThat(result.basicStats.staffType.etcBreakdown)
            .extracting("label", "count")
            .containsExactly(org.assertj.core.groups.Tuple.tuple("기타", 1))
    }

    @Test
    @DisplayName("T10 기타 breakdown — 원본 값별 집계 + null/공백은 '미분류' 합산 + count 내림차순 정렬")
    fun etcBreakdownGrouping() {
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            // 재직/휴직 아님 → 모두 기타. status 원본값별로 그룹핑되어야 함
            employee(status = "파견"), employee(status = "파견"),
            employee(status = "교육"),
            employee(status = null), employee(status = ""),
        )
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val result = service.getDashboard(emptyList(), "2026-05")

        assertThat(result.basicStats.totalByPosition.etc).isEqualTo(5)
        // count 내림차순: 파견 2 / 미분류 2(null+공백) / 교육 1. 동수(파견·미분류)는 라벨 오름차순 → 미분류 먼저
        assertThat(result.basicStats.totalByPosition.etcBreakdown)
            .extracting("label", "count")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("미분류", 2),
                org.assertj.core.groups.Tuple.tuple("파견", 2),
                org.assertj.core.groups.Tuple.tuple("교육", 1),
            )
    }

    @Test
    @DisplayName("T10 빈 데이터 — 3섹션 전부 0 / 빈 리스트 정상 반환")
    fun emptyData() {
        stubEmpty()

        val result = service.getDashboard(emptyList(), "2026-05")

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

        val result = service.getDashboard(emptyList(), null)

        assertThat(result.salesSummary.yearMonth).isEqualTo(expected)
        assertThat(result.staffDeployment.yearMonth).isEqualTo(expected)
    }

    @Test
    @DisplayName("T12 지점 제한(단일 지점) 조회 — branchName 에 본인 지점명 반영")
    fun branchLabelForLeaderScope() {
        stubEmpty()

        val result = service.getDashboard(listOf("1000"), "2026-05", mapOf("1000" to "서울1지점"))

        assertThat(result.salesSummary.branchName).isEqualTo("서울1지점")
        assertThat(result.staffDeployment.branchName).isEqualTo("서울1지점")
        assertThat(result.basicStats.branchName).isEqualTo("서울1지점")
    }

    @Test
    @DisplayName("T13 조회 코드 빈 목록(권한 지점 없음) — branchName '전체'")
    fun branchLabelForEmptyCodes() {
        stubEmpty()

        val result = service.getDashboard(emptyList(), "2026-05", mapOf("1000" to "서울1지점"))

        assertThat(result.salesSummary.branchName).isEqualTo("전체")
    }

    @Test
    @DisplayName("T14 복수 지점 조회 — 'OO 외 N개' 라벨")
    fun branchLabelForMultipleBranches() {
        stubEmpty()

        val result = service.getDashboard(
            listOf("1000", "2000"), "2026-05", mapOf("1000" to "서울1지점", "2000" to "부산지점"),
        )

        assertThat(result.salesSummary.branchName).isEqualTo("서울1지점 외 1개")
    }
}
