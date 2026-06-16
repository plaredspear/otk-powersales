package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
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
    private val service = MonthlySalesAdminQueryService(
        accountRepository,
        monthlySalesHistoryGateway,
        salesProgressRateMasterRepository,
    )

    private val allBranchesScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

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
}
