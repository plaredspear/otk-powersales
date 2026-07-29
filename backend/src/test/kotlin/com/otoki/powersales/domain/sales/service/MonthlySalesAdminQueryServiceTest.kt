package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.schedule.repository.DashboardDeploymentRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.domain.foundation.account.service.AccountCategoryLookupFixture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("MonthlySalesAdminQueryService — RDS 기반 응답 회귀 보호")
class MonthlySalesAdminQueryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository = mockk()
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk()
    /** 유통형태는 거래처유형마스터 조인이 정본이라 운영 마스터 픽스처를 물린다. */
    private val accountCategoryLookup = AccountCategoryLookupFixture.lookup()
    private val service = MonthlySalesAdminQueryService(
        accountRepository,
        monthlySalesHistoryGateway,
        salesProgressRateMasterRepository,
        mfeisRepository,
        accountCategoryLookup,
    )

    private val allBranchesScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    init {
        // 환산인원 조회 기본 stub — 개별 테스트가 override.
        every { mfeisRepository.findDeploymentDashboardRows(any(), any(), any()) } returns emptyList()
    }

    /** MFEIS 투입 row — accountId + workingCategory1(진열/행사) + convertedHeadcount. */
    private fun deploymentRow(accountId: Long, workingCategory1: String, headcount: String) =
        DashboardDeploymentRow(
            convertedHeadcount = BigDecimal(headcount),
            workingCategory1 = workingCategory1,
            workingCategory3 = null,
            workingCategory4 = null,
            accountId = accountId,
            accountExternalKey = "S00$accountId",
            accountType = null,
        )

    // 실제 Account 인스턴스 사용 — mockk mock 에서 accountType 프로퍼티를 read 하면
    // (JPA enum-변환 이력이 얽힌 프로퍼티라) 힙이 폭증하므로, 유통형태/거래처유형 라벨 필드를 읽는
    // getList 경로에서는 실인스턴스로 채운다.
    private fun account(
        id: Long,
        externalKey: String?,
        branchCode: String? = "B001",
        name: String = "거래처$id",
        accountStatusCode: String? = "02",
        accountType: String? = "슈퍼",
        abcTypeCode: String? = "6111",
        abcType: String? = "이마트",
    ): Account = Account(
        id = id,
        externalKey = externalKey,
        name = name,
        branchCode = branchCode,
        branchName = "지점",
        accountStatusCode = accountStatusCode,
        accountType = accountType,
        abcTypeCode = abcTypeCode,
        abcType = abcType,
    )

    /** 실적 row — `account_id` FK 조인 키. closingAmountSum = ABC합 + Ship합 (모바일 「월 매출」 정합). */
    private fun row(
        accountId: Long,
        salesDate: String,
        abc1: Long = 0L,
        ship1: Long = 0L,
        ship2: Long = 0L,
        ship3: Long = 0L,
        ship4: Long = 0L,
    ) = MonthlySalesRow(
        sapAccountCode = "",
        salesDate = salesDate,
        closingAmountSum = BigDecimal(abc1 + ship1 + ship2 + ship3 + ship4),
        accountId = accountId,
        abcClosingAmount1 = BigDecimal(abc1),
        shipClosingAmount1 = BigDecimal(ship1),
        shipClosingAmount2 = BigDecimal(ship2),
        shipClosingAmount3 = BigDecimal(ship3),
        shipClosingAmount4 = BigDecimal(ship4),
    )

    /**
     * 두 축이 **어긋난** 실적 row — 카테고리 축(`ABC_n + Ship_n` 합) ≠ 합계 축(`ClosingAmountSum`).
     *
     * SF 에서 카테고리 8종과 합계 2종은 독립 Number 컬럼이라 실제로 갈릴 수 있다 (ORORA 월마감
     * 인터페이스는 둘을 함께 세팅하지만, 일별 ERP 트리거는 합계만 덮어쓴다). [row] 는 두 축을 항상
     * 같게 만들어 축 구분을 검증할 수 없으므로 전년 산식 회귀 테스트는 이 헬퍼를 쓴다.
     */
    private fun divergentRow(
        accountId: Long,
        salesDate: String,
        categoryAxis: Long,
        sumAxis: Long,
    ) = MonthlySalesRow(
        sapAccountCode = "",
        salesDate = salesDate,
        closingAmountSum = BigDecimal(sumAxis),
        accountId = accountId,
        abcClosingAmount1 = BigDecimal(categoryAxis),
    )

    private fun target(
        month: Int,
        rt: Double = 0.0,
        rm: Double = 0.0,
        fr: Double = 0.0,
        fo: Double = 0.0,
        accountId: Long? = null,
    ): SalesProgressRateMaster =
        mockk {
            every { rtTargetAmount } returns rt
            every { rmTargetAmount } returns rm
            every { frTargetAmount } returns fr
            every { foTargetAmount } returns fo
            every { targetMonth } returns month.toString()
            every { isDeleted } returns false
            if (accountId != null) {
                every { account } returns account(accountId, "S00$accountId")
            }
        }

    @Test
    @DisplayName("getDetail — ClosingAmountSum(ABC+Ship) = achievedAmount, account_id FK 로 조인")
    fun detailSumsClosingAmount() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            row(accountId = 1, salesDate = "202604", abc1 = 500, ship1 = 100, ship2 = 200, ship3 = 100, ship4 = 100),
        )
        every { salesProgressRateMasterRepository.findByAccountIdAndTargetYear(1, "2026") } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(1000L)
        // 목표 미등록 → 0 / 달성률 0
        assertThat(result.targetAmount).isEqualTo(0L)
        assertThat(result.achievementRate).isEqualTo(0.0)
    }

    @Test
    @DisplayName("getDetail — SalesProgressRateMaster 목표 = targetAmount + 달성률 round(실적/목표×100)")
    fun detailRestoresTargetFromProgressRateMaster() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            row(accountId = 1, salesDate = "202604", ship1 = 1000),
        )
        every { salesProgressRateMasterRepository.findByAccountIdAndTargetYear(1, "2026") } returns listOf(
            target(month = 4, rt = 2000.0),
        )

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(1000L)
        assertThat(result.targetAmount).isEqualTo(2000L)
        assertThat(result.achievementRate).isEqualTo(50.0)
    }

    @Test
    @DisplayName("getDetail — RDS row 부재 → achievedAmount = 0")
    fun detailReturnsZeroWhenNoRow() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdAndTargetYear(1, "2026") } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(0L)
        assertThat(result.targetAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getList — SalesProgressRateMaster 목표 = 합계 + 카테고리 4종 (모바일 정합), 달성률 round")
    fun listRestoresTargetWithCategories() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            row(accountId = 1, salesDate = "202604", ship1 = 600, ship2 = 200, ship3 = 100, ship4 = 100),
        )
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026") } returns listOf(
            target(month = 4, rt = 1000.0, rm = 500.0, fr = 300.0, fo = 200.0, accountId = 1),
        )

        val request = MonthlySalesDashboardListRequest(year = 2026, month = 4, costCenterCodes = listOf("B001"))
        val result = service.getList(allBranchesScope, request)

        val item = result.items.single()
        assertThat(item.targetAmount).isEqualTo(2000L) // 1000 + 300 + 500 + 200
        assertThat(item.totalAchievedAmount).isEqualTo(1000L)
        assertThat(item.achievementRate).isEqualTo(50.0)
        assertThat(item.ambientTargetAmount).isEqualTo(1000L)
        assertThat(item.noodleTargetAmount).isEqualTo(500L)
        assertThat(item.frozenRefrigeratedTargetAmount).isEqualTo(300L)
        assertThat(item.oilFatTargetAmount).isEqualTo(200L)
    }

    @Test
    @DisplayName("getList — 실적(마감 합계)은 과거월에도 합계 축, 전년 동월은 카테고리 축 (레거시 Heroku 정합)")
    fun listAxesForPastMonth() {
        // 레거시 요소별 축 (list.jsp 현행 마크업):
        // · 마감 합계 실적(box4) — 항상 합계 축 (`:269` 무조건, 현재월 분기 없음)
        // · 전년 값 — 항상 카테고리 축 `Σ(ABCClosingAmount_n + ShipClosingAmount_n)`
        //   (`:205`, `ShipClosingSumAmount__c` 는 `:206` 에서 명시적 제외)
        // 두 축은 독립 컬럼이라 값이 갈릴 수 있으므로, 두 축이 **다른** row 로 어느 쪽을 읽는지 고정한다.
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            divergentRow(accountId = 1, salesDate = "202004", categoryAxis = 1000, sumAxis = 1500),
            divergentRow(accountId = 1, salesDate = "201904", categoryAxis = 700, sumAxis = 900),
        )
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2020") } returns emptyList()

        // 2020-04 는 항상 과거월 — 실적 축이 조회월에 따라 바뀌지 않음을 시계와 무관하게 검증한다.
        val request = MonthlySalesDashboardListRequest(year = 2020, month = 4, costCenterCodes = listOf("B001"))
        val item = service.getList(allBranchesScope, request).items.single()

        assertThat(item.totalAchievedAmount)
            .withFailMessage("실적(마감 합계)은 과거월에도 합계 축(1500)이어야 한다 — 레거시 box4 는 현재월 분기가 없다")
            .isEqualTo(1500L)
        assertThat(item.lastYearAchievedAmount)
            .withFailMessage("전년 동월은 카테고리 축(700)이어야 한다 — 합계 축(900)을 읽으면 레거시와 어긋난다")
            .isEqualTo(700L)
    }

    @Test
    @DisplayName("getDetail — 실적은 합계 축, 「전년 대비」 차트는 과거월 조회 시 양쪽 모두 카테고리 축")
    fun detailAxesForPastMonth() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            divergentRow(accountId = 1, salesDate = "202001", categoryAxis = 4_000_000, sumAxis = 9_000_000),
            divergentRow(accountId = 1, salesDate = "201901", categoryAxis = 2_000_000, sumAxis = 8_000_000),
        )
        every { salesProgressRateMasterRepository.findByAccountIdAndTargetYear(1, "2020") } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2020, month = 1)

        // 마감 합계 실적 — 항상 합계 축 (레거시 box4)
        assertThat(result.achievedAmount).isEqualTo(9_000_000L)
        // 차트 값 (백만원 단위) — 과거월 조회이므로 당해/전년/평균 모두 카테고리 축.
        // 합계 축을 읽으면 각각 9 / 8 이 된다.
        assertThat(result.yearComparison.currentYear).isEqualTo(4L)
        assertThat(result.yearComparison.previousYear).isEqualTo(2L)
        assertThat(result.monthlyAverage.currentYearAverage).isEqualTo(4L)
        assertThat(result.monthlyAverage.previousYearAverage).isEqualTo(2L)
    }

    @Test
    @DisplayName("getDetail — 현재월 조회는 차트 당해 값도 합계 축, 차트 전년은 그대로 카테고리 축")
    fun detailAxesForCurrentMonth() {
        // 레거시 `list.jsp:253` 은 차트 당해 값에 한해 조회월이 시스템 현재월일 때만 합계 축으로
        // 전환한다 (마감 전 당월은 카테고리 컬럼이 아직 비어 있기 때문). 시계 의존이라 조회 연월을
        // 오늘에서 만든다.
        val today = LocalDate.now()
        val currentSalesDate = "%04d%02d".format(today.year, today.monthValue)
        val lastYearSalesDate = "%04d%02d".format(today.year - 1, today.monthValue)
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            divergentRow(accountId = 1, salesDate = currentSalesDate, categoryAxis = 4_000_000, sumAxis = 9_000_000),
            divergentRow(accountId = 1, salesDate = lastYearSalesDate, categoryAxis = 2_000_000, sumAxis = 8_000_000),
        )
        every {
            salesProgressRateMasterRepository.findByAccountIdAndTargetYear(1, today.year.toString())
        } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = today.year, month = today.monthValue)

        assertThat(result.achievedAmount).isEqualTo(9_000_000L)
        assertThat(result.yearComparison.currentYear)
            .withFailMessage("현재월 차트 당해 값은 합계 축(9)이어야 한다 — 마감 전 당월은 카테고리 컬럼이 비어 있다")
            .isEqualTo(9L)
        assertThat(result.yearComparison.previousYear)
            .withFailMessage("차트 전년 값은 현재월 조회에서도 카테고리 축(2)이어야 한다")
            .isEqualTo(2L)
    }

    @Test
    @DisplayName("getSummary — 목표 합계 = 거래처별 목표 총합, 진도율 round(실적/목표×100)")
    fun summaryRestoresTotalTarget() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns listOf(
            row(accountId = 1, salesDate = "202604", ship1 = 1000),
        )
        // 당월 목표 + 추이용 연도 목표 (동일 연도라 1회 호출되거나 동일 stub 재사용)
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026") } returns listOf(
            target(month = 4, rt = 2000.0, accountId = 1),
        )
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2025") } returns emptyList()

        val result = service.getSummary(
            allBranchesScope, year = 2026, month = 4,
            costCenterCodes = listOf("B001"), customerKeyword = null, accountGroup = null,
        )

        assertThat(result.totalTargetAmount).isEqualTo(2000L)
        assertThat(result.totalAchievedAmount).isEqualTo(1000L)
        assertThat(result.overallAchievementRate).isEqualTo(50.0)
    }

    @Test
    @DisplayName("getList — 진열/행사 환산인원 workingCategory1 별 합산 + 총인원 = 진열 + 행사")
    fun listAggregatesHeadcountByCategory() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026") } returns emptyList()
        // 진열 1.5 + 1.0 = 2.5, 행사 0.75 → 상시/임시·위탁 무필터 전체 합산
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", listOf("B001")) } returns listOf(
            deploymentRow(1, "진열", "1.5"),
            deploymentRow(1, "진열", "1.0"),
            deploymentRow(1, "행사", "0.75"),
        )

        val request = MonthlySalesDashboardListRequest(year = 2026, month = 4, costCenterCodes = listOf("B001"))
        val item = service.getList(allBranchesScope, request).items.single()

        assertThat(item.displayHeadcount).isEqualByComparingTo("2.5")
        assertThat(item.eventHeadcount).isEqualByComparingTo("0.75")
        assertThat(item.totalHeadcount).isEqualByComparingTo("3.25")
    }

    @Test
    @DisplayName("getList — deploymentFilter='deployed' 는 근무등록(MFEIS keySet) 거래처만 남긴다")
    fun listFiltersDeployedAccountsOnly() {
        val accA = account(1, "S001") // MFEIS 존재 (근무등록)
        val accB = account(2, "S002") // MFEIS 없음 (미등록)
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accA, accB)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", listOf("B001")) } returns listOf(
            deploymentRow(1, "진열", "1.0"),
        )

        val deployed = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), deploymentFilter = "deployed"),
        )
        assertThat(deployed.items.map { it.accountId }).containsExactly(1L)

        val undeployed = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), deploymentFilter = "undeployed"),
        )
        assertThat(undeployed.items.map { it.accountId }).containsExactly(2L)

        val all = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), deploymentFilter = null),
        )
        assertThat(all.items.map { it.accountId }).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    @DisplayName("getSummary — deploymentFilter='deployed' 는 근무등록 거래처만 목표/실적 누계에 합산")
    fun summaryAggregatesDeployedAccountsOnly() {
        val accA = account(1, "S001") // 근무등록 + 실적 1000 + 목표 2000
        val accB = account(2, "S002") // 미등록 + 실적 500 + 목표 3000
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accA, accB)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(
            row(accountId = 1, salesDate = "202604", ship1 = 1000),
            row(accountId = 2, salesDate = "202604", ship1 = 500),
        )
        // 추이(6개월)용 이전 연도 조회는 목표 없음, 당월 연도(2026)만 목표 존재
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), "2026") } returns listOf(
            target(month = 4, rt = 2000.0, accountId = 1),
            target(month = 4, rt = 3000.0, accountId = 2),
        )
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", listOf("B001")) } returns listOf(
            deploymentRow(1, "진열", "1.0"),
        )

        val result = service.getSummary(
            allBranchesScope, year = 2026, month = 4,
            costCenterCodes = listOf("B001"), customerKeyword = null, accountGroup = null,
            deploymentFilter = "deployed",
        )

        // 근무등록된 거래처(1)만 반영 — 거래처(2) 실적/목표는 제외
        assertThat(result.totalAchievedAmount).isEqualTo(1000L)
        assertThat(result.totalTargetAmount).isEqualTo(2000L)
        assertThat(result.overallAchievementRate).isEqualTo(50.0)
    }

    @Test
    @DisplayName("getList — 여사원 투입 없는 거래처는 진열/행사/총인원 모두 0")
    fun listHeadcountZeroWhenNoDeployment() {
        val acc = account(2, "S002")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(2L)) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(2L), "2026") } returns emptyList()
        // 다른 거래처(1) 투입만 존재 → 조회 거래처(2)는 매핑 없음
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", listOf("B001")) } returns listOf(
            deploymentRow(1, "진열", "1.0"),
        )

        val request = MonthlySalesDashboardListRequest(year = 2026, month = 4, costCenterCodes = listOf("B001"))
        val item = service.getList(allBranchesScope, request).items.single()

        assertThat(item.displayHeadcount).isEqualByComparingTo("0")
        assertThat(item.eventHeadcount).isEqualByComparingTo("0")
        assertThat(item.totalHeadcount).isEqualByComparingTo("0")
    }

    @Test
    @DisplayName("getList — 진열/행사 외 workingCategory1 값은 진열·행사·총인원 어디에도 포함하지 않음")
    fun listHeadcountIgnoresUnknownCategory() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(1L)) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026") } returns emptyList()
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", listOf("B001")) } returns listOf(
            deploymentRow(1, "진열", "1.0"),
            deploymentRow(1, "기타", "9.0"),
            deploymentRow(1, "", "9.0"),
        )

        val request = MonthlySalesDashboardListRequest(year = 2026, month = 4, costCenterCodes = listOf("B001"))
        val item = service.getList(allBranchesScope, request).items.single()

        assertThat(item.displayHeadcount).isEqualByComparingTo("1.0")
        assertThat(item.eventHeadcount).isEqualByComparingTo("0")
        assertThat(item.totalHeadcount).isEqualByComparingTo("1.0")
    }

    @Test
    @DisplayName("getListForExport — 결과가 EXPORT_MAX_ROWS(50000) 로 절단된다")
    fun exportCapsAtMaxRows() {
        val accounts = (1..50_001L).map { account(it, "S$it") }
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns accounts
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()

        val request = MonthlySalesDashboardListRequest(year = 2026, month = 4, costCenterCodes = listOf("B001"))
        val result = service.getListForExport(allBranchesScope, request)

        assertThat(result).hasSize(50_000)
    }

    @Test
    @DisplayName("getList — customerKeyword 는 거래처명 OR 거래처코드(externalKey) 부분일치")
    fun listFiltersByCustomerNameOrCode() {
        val accA = account(1, "SAP-ALPHA", name = "가나다마트")
        val accB = account(2, "SAP-BETA", name = "라마바상점")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accA, accB)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()

        // 거래처명 부분일치
        val byName = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), customerKeyword = "가나다"),
        )
        assertThat(byName.items.map { it.accountId }).containsExactly(1L)

        // 거래처코드(externalKey) 부분일치 (대소문자 무시)
        val byCode = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), customerKeyword = "beta"),
        )
        assertThat(byCode.items.map { it.accountId }).containsExactly(2L)
    }

    @Test
    @DisplayName("getList — distributionChannels 는 거래처유형마스터 코드 다중 매칭 (POS매출 정합)")
    fun listFiltersByDistribution() {
        // 거래처상태코드는 유통형태와 무관한 축이라 일부러 유형과 어긋나게 둔다 — 매칭에 영향이 없어야 한다.
        val accA = account(1, "S001", accountStatusCode = "02", accountType = "슈퍼") // 06 슈퍼
        val accB = account(2, "S002", accountStatusCode = "05", accountType = "체인") // 02 체인
        val accC = account(3, "S003", accountStatusCode = "01", accountType = "대형마트(3대)") // 01 대형마트(3대)
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accA, accB, accC)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()

        // 단일 코드 매칭 — "02"(체인) → accB. 상태코드 02 인 accA(슈퍼)는 걸리지 않는다.
        val single = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), distributionChannels = listOf("02")),
        )
        assertThat(single.items.map { it.accountId }).containsExactly(2L)
        assertThat(single.items.first().distributionChannelLabel).isEqualTo("02 체인")

        // 다중 코드 매칭(합집합) — "06"(슈퍼) OR "01"(대형마트(3대)) → accA, accC
        val multi = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(
                2026, 4, listOf("B001"),
                distributionChannels = listOf("06", "01"),
            ),
        )
        assertThat(multi.items.map { it.accountId }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    @DisplayName("getList — accountTypes 는 거래처유형 라벨 다중 정확일치 (POS매출 정합)")
    fun listFiltersByAccountType() {
        val accA = account(1, "S001", abcTypeCode = "6111", abcType = "이마트") // "6111 이마트"
        val accB = account(2, "S002", abcTypeCode = "2001", abcType = "일반슈퍼") // "2001 일반슈퍼"
        val accC = account(3, "S003", abcTypeCode = "6112", abcType = "홈플러스") // "6112 홈플러스"
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accA, accB, accC)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), any()) } returns emptyList()

        // 단일 라벨 정확일치
        val single = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), accountTypes = listOf("2001 일반슈퍼")),
        )
        assertThat(single.items.map { it.accountId }).containsExactly(2L)

        // 다중 라벨 정확일치(합집합)
        val multi = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(
                2026, 4, listOf("B001"),
                accountTypes = listOf("6111 이마트", "6112 홈플러스"),
            ),
        )
        assertThat(multi.items.map { it.accountId }).containsExactlyInAnyOrder(1L, 3L)
    }

    @Test
    @DisplayName("getList — targetRegistration 은 목표 row 존재유무 기준 (registered/unregistered)")
    fun listFiltersByTargetRegistration() {
        val accWithTarget = account(1, "S001")
        val accNoTarget = account(2, "S002")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(accWithTarget, accNoTarget)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()
        // 거래처 1 만 (2026, 4) 목표 row 존재 — 거래처 2 는 목표 미등록. 금액 0 이어도 row 있으면 등록.
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(any(), "2026") } returns listOf(
            target(month = 4, rt = 0.0, accountId = 1),
        )

        val registered = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), targetRegistration = "registered"),
        )
        assertThat(registered.items.map { it.accountId }).containsExactly(1L)

        val unregistered = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001"), targetRegistration = "unregistered"),
        )
        assertThat(unregistered.items.map { it.accountId }).containsExactly(2L)

        // 필터 없으면 전체
        val all = service.getList(
            allBranchesScope,
            MonthlySalesDashboardListRequest(2026, 4, listOf("B001")),
        )
        assertThat(all.items.map { it.accountId }).containsExactlyInAnyOrder(1L, 2L)
    }

    /** sapAccountCode 를 지정한 실적 row — sumInvestedAccountSales 의 (externalKey, salesDate) 키 매칭용. */
    private fun rowByCode(
        sapAccountCode: String,
        salesDate: String,
        accountId: Long,
        abc1: Long = 0L,
        ship1: Long = 0L,
        ship2: Long = 0L,
        ship3: Long = 0L,
        ship4: Long = 0L,
    ) = MonthlySalesRow(
        sapAccountCode = sapAccountCode,
        salesDate = salesDate,
        closingAmountSum = BigDecimal(abc1 + ship1 + ship2 + ship3 + ship4),
        accountId = accountId,
        abcClosingAmount1 = BigDecimal(abc1),
        shipClosingAmount1 = BigDecimal(ship1),
        shipClosingAmount2 = BigDecimal(ship2),
        shipClosingAmount3 = BigDecimal(ship3),
        shipClosingAmount4 = BigDecimal(ship4),
    )

    @Test
    @DisplayName("sumInvestedAccountSales — 당월/전년 실적 = ClosingAmountSum(ABC+Ship 합계), 레거시 정합")
    fun sumInvestedAccountSalesUsesAbcPlusShip() {
        val refs = listOf(
            MonthlySalesAdminQueryService.InvestedAccountRef(id = 1L, externalKey = "S001"),
        )
        // 당월(202605): ABC 500 + Ship(100+200+100+100=500) = 1000
        // 전년(202505): ABC 300 + Ship(50+50+50+50=200) = 500
        every {
            monthlySalesHistoryGateway.findBySalesDates(listOf("202605", "202505"), listOf("S001"))
        } returns listOf(
            rowByCode("S001", "202605", accountId = 1, abc1 = 500, ship1 = 100, ship2 = 200, ship3 = 100, ship4 = 100),
            rowByCode("S001", "202505", accountId = 1, abc1 = 300, ship1 = 50, ship2 = 50, ship3 = 50, ship4 = 50),
        )
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(1L), "2026") } returns emptyList()

        val result = service.sumInvestedAccountSales(refs, year = 2026, month = 5)

        // Ship 단독(500)이 아니라 ABC+Ship 합계(1000) 여야 한다.
        assertThat(result.actualAmount).isEqualTo(1000L)
        assertThat(result.lastYearAmount).isEqualTo(500L)
        assertThat(result.hasActualData).isTrue()
        assertThat(result.hasLastYearData).isTrue()
    }
}
