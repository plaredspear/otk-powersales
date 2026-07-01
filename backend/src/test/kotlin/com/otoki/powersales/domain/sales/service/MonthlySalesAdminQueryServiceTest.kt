package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.schedule.repository.DashboardDeploymentRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("MonthlySalesAdminQueryService — RDS 기반 응답 회귀 보호")
class MonthlySalesAdminQueryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository = mockk()
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk()
    private val service = MonthlySalesAdminQueryService(
        accountRepository,
        monthlySalesHistoryGateway,
        salesProgressRateMasterRepository,
        mfeisRepository,
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
            accountId = accountId,
            accountExternalKey = "S00$accountId",
            accountType = null,
        )

    private fun account(id: Long, externalKey: String?, branchCode: String? = "B001"): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
        every { this@mockk.name } returns "거래처$id"
        every { this@mockk.branchCode } returns branchCode
        every { this@mockk.branchName } returns "지점"
    }

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
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", emptyList()) } returns listOf(
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
    @DisplayName("getList — 여사원 투입 없는 거래처는 진열/행사/총인원 모두 0")
    fun listHeadcountZeroWhenNoDeployment() {
        val acc = account(2, "S002")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), listOf(2L)) } returns emptyList()
        every { salesProgressRateMasterRepository.findByAccountIdInAndTargetYear(listOf(2L), "2026") } returns emptyList()
        // 다른 거래처(1) 투입만 존재 → 조회 거래처(2)는 매핑 없음
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", emptyList()) } returns listOf(
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
        every { mfeisRepository.findDeploymentDashboardRows("2026", "4", emptyList()) } returns listOf(
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
}
