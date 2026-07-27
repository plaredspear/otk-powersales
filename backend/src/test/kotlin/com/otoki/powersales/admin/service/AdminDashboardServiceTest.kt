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
import com.otoki.powersales.platform.common.util.TimeZones
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

@DisplayName("AdminDashboardService 테스트 (실집계)")
class AdminDashboardServiceTest {

    private val mfeisRepository = mockk<MonthlyFemaleEmployeeIntegrationScheduleRepository>()
    private val employeeRepository = mockk<EmployeeRepository>()
    private val monthlySalesAdminQueryService = mockk<MonthlySalesAdminQueryService>()

    /**
     * 고정 시계 — 기본 현황 기준일(전일) 검증을 결정론적으로 만든다.
     * KST 2026-05-20 09:00 로 고정하므로 기준일은 항상 [expectedAsOfDate] (2026-05-19).
     */
    private val fixedClock: Clock = Clock.fixed(
        ZonedDateTime.of(2026, 5, 20, 9, 0, 0, 0, TimeZones.SEOUL_ZONE).toInstant(),
        TimeZones.SEOUL_ZONE,
    )

    /** [fixedClock] 기준 전일. */
    private val expectedAsOfDate: LocalDate = LocalDate.of(2026, 5, 19)

    private val service = AdminDashboardService(
        mfeisRepository, employeeRepository, monthlySalesAdminQueryService, fixedClock,
    )

    // -- fixtures --

    /** 투입 거래처 식별 — (id, accountType) 쌍. externalKey 는 "SAP{id}" 규칙. */
    private fun account(id: Long, type: String): Pair<Long, String> = id to type

    private fun mfeis(
        accountType: String? = "슈퍼",
        wc1: String? = "진열",
        wc3: String? = "고정",
        wc4: String? = null,
        headcount: BigDecimal = BigDecimal.ONE,
        acc: Pair<Long, String>? = null,
    ): DashboardDeploymentRow {
        val resolved = acc ?: accountType?.let { account(1, it) }
        return DashboardDeploymentRow(
            convertedHeadcount = headcount,
            workingCategory1 = wc1,
            workingCategory3 = wc3,
            workingCategory4 = wc4,
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
        jikchak: String? = null,
        jikwee: String? = null,
    ): DashboardEmployeeProjection {
        empSeq++
        val s = status; val j = jobCode; val b = birthDate; val jc = jikchak; val jw = jikwee
        return object : DashboardEmployeeProjection {
            override val status = s
            override val jobCode = j
            override val birthDate = b
            override val jikchak = jc
            override val jikwee = jw
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
    @DisplayName("T1 진열 차트 — 유통×근무형태3 스택 (슈퍼 고정 400 / 순회 54.9, 농협 격고 30), 전월 마감 기준")
    fun displayChartChannelStack() {
        val superAcc = account(1, "슈퍼")
        val nhAcc = account(2, "농협")
        val rows = listOf(
            mfeis(wc1 = "진열", wc3 = "고정", headcount = BigDecimal("400"), acc = superAcc),
            mfeis(wc1 = "진열", wc3 = "순회", headcount = BigDecimal("54.9"), acc = superAcc),
            mfeis(wc1 = "진열", wc3 = "격고", headcount = BigDecimal("30"), acc = nhAcc),
            // 행사 row 는 진열 차트에 미포함
            mfeis(wc1 = "행사", wc4 = "냉동", headcount = BigDecimal("999"), acc = superAcc),
        )
        // 투입현황 차트는 전월(마감) 기준 → previousYm rows 로 반환
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val display = service.getDashboard(emptyList(), "2026-05").staffDeployment.display

        // 스택 키는 preset 순서 (등장 라벨만): 1.고정, 2.격고, 3.순회
        assertThat(display.stackKeys).containsExactly("1.고정", "2.격고", "3.순회")
        val superRow = display.rows.first { it.channelName == "슈퍼" }
        assertThat(superRow.headcounts[0]).isEqualByComparingTo(BigDecimal("400.0000")) // 1.고정
        assertThat(superRow.headcounts[1]).isEqualByComparingTo(BigDecimal("0.0000")) // 2.격고
        assertThat(superRow.headcounts[2]).isEqualByComparingTo(BigDecimal("54.9000")) // 3.순회
        val nhRow = display.rows.first { it.channelName == "농협" }
        assertThat(nhRow.headcounts[1]).isEqualByComparingTo(BigDecimal("30.0000"))
        // 총합 = 400 + 54.9 + 30 (행사 999 미포함)
        assertThat(display.totalHeadcount).isEqualByComparingTo(BigDecimal("484.9000"))
    }

    @Test
    @DisplayName("T2 행사 차트 — 근무형태4 스택 (4.상온 / 5.냉동), 상온은 4.상온 그 외 5.{값}, 전월 마감 기준")
    fun eventChartWorkType4Stack() {
        val superAcc = account(1, "슈퍼")
        val rows = listOf(
            mfeis(wc1 = "행사", wc4 = "상온", headcount = BigDecimal("100"), acc = superAcc),
            mfeis(wc1 = "행사", wc4 = "냉동", headcount = BigDecimal("60"), acc = superAcc),
            mfeis(wc1 = "행사", wc4 = "만두", headcount = BigDecimal("40"), acc = superAcc),
            // 진열 row 는 행사 차트에 미포함
            mfeis(wc1 = "진열", wc3 = "고정", headcount = BigDecimal("999"), acc = superAcc),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val event = service.getDashboard(emptyList(), "2026-05").staffDeployment.event

        // preset 순서 (4.상온, 5.냉동, 5.만두 — 5.냉장/5.라면 미등장). 만두는 preset 뒤쪽.
        assertThat(event.stackKeys).containsExactly("4.상온", "5.냉동", "5.만두")
        val superRow = event.rows.first { it.channelName == "슈퍼" }
        assertThat(superRow.headcounts[0]).isEqualByComparingTo(BigDecimal("100.0000")) // 4.상온
        assertThat(superRow.headcounts[1]).isEqualByComparingTo(BigDecimal("60.0000")) // 5.냉동
        assertThat(superRow.headcounts[2]).isEqualByComparingTo(BigDecimal("40.0000")) // 5.만두
        assertThat(event.totalHeadcount).isEqualByComparingTo(BigDecimal("200.0000"))
    }

    @Test
    @DisplayName("T3 행사 근무형태4 미지정값(신규) — preset 밖 라벨은 stackKeys 뒤에 이름순으로 붙음")
    fun eventChartUnknownWc4() {
        val superAcc = account(1, "슈퍼")
        val rows = listOf(
            mfeis(wc1 = "행사", wc4 = "상온", headcount = BigDecimal("10"), acc = superAcc),
            mfeis(wc1 = "행사", wc4 = "신규품목", headcount = BigDecimal("5"), acc = superAcc),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val event = service.getDashboard(emptyList(), "2026-05").staffDeployment.event

        // preset(4.상온) 먼저 + preset 밖("5.신규품목") 뒤에
        assertThat(event.stackKeys).containsExactly("4.상온", "5.신규품목")
    }

    @Test
    @DisplayName("T4 ①거래처유형별 + ②유통×진열/행사 + ③진열/행사 도넛 + ④All 누적 — 진열+행사 합산, 전월 기준")
    fun sixChartAggregation() {
        val superAcc = account(1, "슈퍼")
        val nhAcc = account(2, "농협")
        val rows = listOf(
            mfeis(wc1 = "진열", wc3 = "고정", headcount = BigDecimal("400"), acc = superAcc),
            mfeis(wc1 = "행사", wc4 = "냉동", headcount = BigDecimal("100"), acc = superAcc),
            mfeis(wc1 = "진열", wc3 = "격고", headcount = BigDecimal("30"), acc = nhAcc),
            mfeis(wc1 = "행사", wc4 = "상온", headcount = BigDecimal("20"), acc = nhAcc),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", any()) } returns rows
        every { mfeisRepository.findDeploymentDashboardRows("2026", "5", any()) } returns emptyList()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns emptyList()
        every { monthlySalesAdminQueryService.sumInvestedAccountSales(any(), any(), any()) } returns
            MonthlySalesAdminQueryService.InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false, hasTargetData = false,
            )

        val sd = service.getDashboard(emptyList(), "2026-05").staffDeployment

        // ① 거래처유형별 (진열+행사 합): 슈퍼 400+100=500, 농협 30+20=50
        val byType = sd.byAccountType.associateBy { it.accountType }
        assertThat(byType["슈퍼"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("500.0000"))
        assertThat(byType["농협"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("50.0000"))

        // ② 유통 × 진열/행사: 슈퍼 진열 400 / 행사 100
        assertThat(sd.channelWorkType1.stackKeys).containsExactly("진열", "행사")
        val superCwt = sd.channelWorkType1.rows.first { it.channelName == "슈퍼" }
        assertThat(superCwt.headcounts[0]).isEqualByComparingTo(BigDecimal("400.0000")) // 진열
        assertThat(superCwt.headcounts[1]).isEqualByComparingTo(BigDecimal("100.0000")) // 행사

        // ③ 진열/행사 도넛: 진열 430(400+30) / 행사 120(100+20)
        val ratio = sd.workType1Ratio.associateBy { it.workType }
        assertThat(ratio["진열"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("430.0000"))
        assertThat(ratio["행사"]!!.convertedHeadcount).isEqualByComparingTo(BigDecimal("120.0000"))

        // ④ All 누적 (근무형태3&4 전체): 슈퍼 1.고정 400 + 5.냉동 100
        assertThat(sd.all.stackKeys).contains("1.고정", "2.격고", "4.상온", "5.냉동")
        val superAll = sd.all.rows.first { it.channelName == "슈퍼" }
        val fixedIdx = sd.all.stackKeys.indexOf("1.고정")
        val frozenIdx = sd.all.stackKeys.indexOf("5.냉동")
        assertThat(superAll.headcounts[fixedIdx]).isEqualByComparingTo(BigDecimal("400.0000"))
        assertThat(superAll.headcounts[frozenIdx]).isEqualByComparingTo(BigDecimal("100.0000"))
        assertThat(sd.all.totalHeadcount).isEqualByComparingTo(BigDecimal("550.0000"))
    }

    // T3-1 기본현황 근무형태별(고정/격고/순회) 환산인원 SUM 테스트는 제거됨 —
    // 해당 차트가 기준 시점 혼선(현재 시점 vs 선택월)으로 기본 현황에서 빠졌다.
    // 근무형태별 환산인원 집계 검증은 여사원 투입현황(staffDeployment) 테스트가 담당한다.

    @Test
    @DisplayName("직급별 인원현황 — 판매조장은 jikchak 축으로 먼저 떼어내고 실제 직위를 동적 노출한다")
    fun rankGroupsLeaderUsesJikchakAndDynamicRanks() {
        stubEmpty()
        // 조장도 jobCode 는 판촉직이라, 판매조장을 먼저 분리하지 않으면 판촉직 열에 중복 계상된다.
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", jikchak = "판매조장", jikwee = "OSPM"),
            employee(jobCode = "판촉직", jikchak = "판매조장", jikwee = "주임"),
            employee(jobCode = "판촉직", jikchak = "판매조장", jikwee = "주임"),
            employee(jobCode = "판촉직", jikchak = null, jikwee = "OSPJ"),
        )

        val byRank = service.getDashboard(emptyList(), "2026-05").basicStats.active.byRank

        val leader = byRank.first { it.group == "판매조장" }
        // 인원수 내림차순 — 주임 2 가 OSPM 1 보다 앞
        assertThat(leader.ranks.map { it.label to it.count })
            .containsExactly("주임" to 2, "OSPM" to 1)
        // 조장 3명은 판촉직 열에 중복 계상되지 않는다
        val promotion = byRank.first { it.group == "판촉직" }
        assertThat(promotion.ranks.first { it.label == "OSPM" }.count).isEqualTo(0)
        assertThat(promotion.ranks.first { it.label == "OSPJ" }.count).isEqualTo(1)
    }

    @Test
    @DisplayName("직급별 인원현황 — 판촉직/OSC직은 표준 직위 4개를 고정 노출하고 그 외는 '기타' 합산")
    fun rankGroupsFixedColumnsWithEtc() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", jikwee = "OSPM"),
            employee(jobCode = "판촉직", jikwee = "수습사원"),
            employee(jobCode = "판촉직", jikwee = null),
            employee(jobCode = "OSC직", jikwee = "OSC"),
            // 레이디직(구 OSC)은 OSC직 그룹에 합산된다
            employee(jobCode = "레이디직", jikwee = "OSC"),
        )

        val byRank = service.getDashboard(emptyList(), "2026-05").basicStats.active.byRank

        // 직위는 직무에 종속된다 — 판촉직 열에 OSC 가 붙지 않는다.
        val promotion = byRank.first { it.group == "판촉직" }
        assertThat(promotion.ranks.map { it.label })
            .containsExactly("OSPM", "OSPE", "OSPJ", "기타")
        assertThat(promotion.ranks.first { it.label == "기타" }.count).isEqualTo(2)

        // OSC직 열에도 OSPM/OSPE/OSPJ 가 붙지 않는다.
        val osc = byRank.first { it.group == "OSC직" }
        assertThat(osc.ranks.map { it.label }).containsExactly("OSC")
        assertThat(osc.ranks.first { it.label == "OSC" }.count).isEqualTo(2)
    }

    @Test
    @DisplayName("직급별 인원현황 — 직무별 열 집합이 다르다 (판촉직 OSPM·OSPE·OSPJ / OSC직 OSC)")
    fun rankGroupsColumnsDifferPerJobCode() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", jikwee = "OSPM"),
            employee(jobCode = "OSC직", jikwee = "OSC"),
        )

        val byRank = service.getDashboard(emptyList(), "2026-05").basicStats.active.byRank

        // 두 그룹에 전체 직위를 일괄 노출하면 항상 0인 열이 생긴다 — 그룹별 열 집합으로 방지.
        assertThat(byRank.first { it.group == "판촉직" }.ranks.map { it.label })
            .doesNotContain("OSC")
        assertThat(byRank.first { it.group == "OSC직" }.ranks.map { it.label })
            .doesNotContain("OSPM", "OSPE", "OSPJ")
    }

    @Test
    @DisplayName("직급별 인원현황 — 판매조장은 판촉직에서만 선임되므로 OSC직 열에 조장이 섞이지 않는다")
    fun rankGroupsLeaderNeverAppearsInOsc() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", jikchak = "판매조장", jikwee = "주임"),
            employee(jobCode = "판촉직", jikwee = "OSPM"),
            employee(jobCode = "OSC직", jikwee = "OSC"),
        )

        val byRank = service.getDashboard(emptyList(), "2026-05").basicStats.active.byRank

        // OSC직 열은 조장을 제외한 순수 OSC직 인원만
        val osc = byRank.first { it.group == "OSC직" }
        assertThat(osc.ranks.sumOf { it.count }).isEqualTo(1)
        // 판촉직 열에도 조장이 중복되지 않는다
        val promotion = byRank.first { it.group == "판촉직" }
        assertThat(promotion.ranks.sumOf { it.count }).isEqualTo(1)
        // 총합계는 모수 3명과 일치 (조장 1 + 판촉직 1 + OSC직 1)
        assertThat(byRank.sumOf { g -> g.ranks.sumOf { it.count } }).isEqualTo(3)
    }

    @Test
    @DisplayName("직급별 인원현황 — 재직자만 집계 (휴직·미분류는 제외, 같은 탭 다른 차트와 모수가 다름)")
    fun rankGroupsCountsActiveOnly() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(status = "재직", jobCode = "판촉직", jikwee = "OSPM"),
            employee(status = "휴직", jobCode = "판촉직", jikwee = "OSPM"),
            employee(status = null, jobCode = "판촉직", jikwee = "OSPM"),
            employee(status = "휴직", jobCode = "판촉직", jikchak = "판매조장", jikwee = "주임"),
        )

        val basic = service.getDashboard(emptyList(), "2026-05").basicStats

        // 직급별 표는 재직 1명만
        assertThat(basic.active.byRank.sumOf { g -> g.ranks.sumOf { it.count } }).isEqualTo(1)
        // 휴직 조장도 제외되므로 판매조장 그룹 자체가 나오지 않는다
        assertThat(basic.active.byRank.map { it.group }).containsExactly("판촉직")
        // 같은 탭의 다른 차트는 휴직 포함 모수를 유지한다 (퇴직만 제외)
        assertThat(basic.totalByPosition.active + basic.totalByPosition.onLeave + basic.totalByPosition.etc)
            .isEqualTo(4)
    }

    @Test
    @DisplayName("직급별 인원현황 — 인원 0인 그룹은 표에서 제외한다")
    fun rankGroupsOmitsEmptyGroups() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", jikwee = "OSPM"),
        )

        val byRank = service.getDashboard(emptyList(), "2026-05").basicStats.active.byRank

        assertThat(byRank.map { it.group }).containsExactly("판촉직")
    }

    @Test
    @DisplayName("집계 기준 토글 — active 는 재직만, includingLeave 는 휴직 포함. 총원 카드는 항상 전체")
    fun basicStatsProvidesBothScopes() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(status = "재직", jobCode = "판촉직", jikwee = "OSPM"),
            employee(status = "재직", jobCode = "OSC직", jikwee = "OSC"),
            employee(status = "휴직", jobCode = "판촉직", jikwee = "OSPJ"),
        )

        val basic = service.getDashboard(emptyList(), "2026-05").basicStats

        // 재직 기준 — 판촉직 1 + OSC직 1
        assertThat(basic.active.staffType.promotion).isEqualTo(1)
        assertThat(basic.active.byRank.sumOf { g -> g.ranks.sumOf { it.count } }).isEqualTo(2)
        // 재직+휴직 기준 — 판촉직 2 + OSC직 1
        assertThat(basic.includingLeave.staffType.promotion).isEqualTo(2)
        assertThat(basic.includingLeave.byRank.sumOf { g -> g.ranks.sumOf { it.count } }).isEqualTo(3)
        // 총원 카드는 토글과 무관하게 항상 전체 — 좁히면 휴직 세그먼트가 0이 되어 무의미해진다
        assertThat(basic.totalByPosition.active).isEqualTo(2)
        assertThat(basic.totalByPosition.onLeave).isEqualTo(1)
    }

    @Test
    @DisplayName("평균연령 — 생년월일 없는 사원은 모수에서 제외하고 소수 1자리로 반올림한다")
    fun averageAgeExcludesUnknownBirthDate() {
        stubEmpty()
        // 기준일 2026-05-31(선택월 말일). 만 40 / 만 45 / 생년월일 없음
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", birthDate = "1986-01-01"),
            employee(jobCode = "판촉직", birthDate = "1981-01-01"),
            employee(jobCode = "판촉직", birthDate = null),
        )

        val basic = service.getDashboard(emptyList(), "2026-05").basicStats

        // (40 + 45) / 2 = 42.5 — 생년월일 없는 1명을 0살로 계상하면 28.3 이 되어버린다
        assertThat(basic.active.averageAge).isEqualByComparingTo(BigDecimal("42.5"))
        // 연령별 버킷에는 '미상' 으로 남는다 (평균에서만 제외)
        assertThat(basic.active.byAgeGroup.map { it.ageGroup }).contains("미상")
    }

    @Test
    @DisplayName("평균연령 — 생년월일이 있는 사원이 하나도 없으면 null")
    fun averageAgeNullWhenNoBirthDate() {
        stubEmpty()
        every { employeeRepository.findDashboardBasicStatsProjection(any()) } returns listOf(
            employee(jobCode = "판촉직", birthDate = null),
        )

        assertThat(service.getDashboard(emptyList(), "2026-05").basicStats.active.averageAge).isNull()
    }

    @Test
    @DisplayName("기본현황 기준일은 서버 KST 전일 — 조회월과 무관하게 고정 시계 기준 전일을 내려준다")
    fun basicStatsAsOfDateIsPreviousDayInSeoul() {
        stubEmpty()

        // 조회월을 과거로 바꿔도 기준일은 서버 '오늘'의 전일이다 (기본현황은 현재 상태 스냅샷).
        val current = service.getDashboard(emptyList(), "2026-05")
        val past = service.getDashboard(emptyList(), "2026-01")

        assertThat(current.basicStats.asOfDate).isEqualTo(expectedAsOfDate)
        assertThat(past.basicStats.asOfDate).isEqualTo(expectedAsOfDate)
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
        val byAge = result.basicStats.active.byAgeGroup.associateBy { it.ageGroup }

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

        assertThat(result.basicStats.active.byAgeGroup.first { it.ageGroup == "미상" }.count).isEqualTo(1)
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

        assertThat(result.basicStats.active.staffType.promotion).isEqualTo(2)
        assertThat(result.basicStats.active.staffType.osc).isEqualTo(2)
        assertThat(result.basicStats.active.staffType.etc).isEqualTo(1)
        // 기타(jobCode="기타") → "기타" 1명 breakdown
        assertThat(result.basicStats.active.staffType.etcBreakdown)
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
        assertThat(result.staffDeployment.display.rows).isEmpty()
        assertThat(result.staffDeployment.display.stackKeys).isEmpty()
        assertThat(result.staffDeployment.event.rows).isEmpty()
        assertThat(result.staffDeployment.display.totalHeadcount).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.basicStats.active.staffType.promotion).isZero()
        assertThat(result.basicStats.active.byAgeGroup).isEmpty()
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
