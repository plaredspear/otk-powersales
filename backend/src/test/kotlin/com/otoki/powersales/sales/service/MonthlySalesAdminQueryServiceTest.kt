package com.otoki.powersales.sales.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("MonthlySalesAdminQueryService 테스트")
class MonthlySalesAdminQueryServiceTest {

    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk()
    private val accountRepository: AccountRepository = mockk()

    private val service = MonthlySalesAdminQueryService(
        monthlySalesHistoryRepository,
        accountRepository,
    )

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun account(id: Int, name: String, branchCode: String): Account {
        val acc = Account(id = id, sfid = "ACC$id")
        acc.name = name
        acc.branchCode = branchCode
        acc.branchName = "지점$branchCode"
        return acc
    }

    private fun history(
        account: Account,
        year: SalesYear = SalesYear.Y2026,
        month: SalesMonth = SalesMonth.M05,
        target: Long? = 1_000_000L,
        ship1: Double = 100_000.0,
        ship2: Double = 100_000.0,
        ship3: Double = 100_000.0,
        ship4: Double = 100_000.0,
        shipSum: Double? = null,
        abc1: Double = 90_000.0,
        abc2: Double = 90_000.0,
        abc3: Double = 90_000.0,
        abc4: Double = 90_000.0,
        confirmed: Boolean? = null,
    ): MonthlySalesHistory {
        val row = MonthlySalesHistory(
            id = (account.id * 100 + (month.value.toInt())).toLong(),
            salesYear = year,
            salesMonth = month,
        )
        row.account = account
        row.thisMonthTarget = target?.let { BigDecimal.valueOf(it) }
        row.shipClosingAmount1 = ship1
        row.shipClosingAmount2 = ship2
        row.shipClosingAmount3 = ship3
        row.shipClosingAmount4 = ship4
        row.shipClosingSumAmount = shipSum ?: (ship1 + ship2 + ship3 + ship4)
        row.abcClosingAmount1 = abc1
        row.abcClosingAmount2 = abc2
        row.abcClosingAmount3 = abc3
        row.abcClosingAmount4 = abc4
        row.isConfirmed = confirmed
        row.sapAccountCode = "SAP${account.id}"
        return row
    }

    @BeforeEach
    fun resetMocks() {
        // 기본 stub — 모든 repo 호출에 빈 결과 (각 테스트가 필요 시 override)
        every { monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), any()) } returns emptyList()
        every { monthlySalesHistoryRepository.findBySalesYearAndSalesMonthInAndAccountIn(any(), any(), any()) } returns emptyList()
    }

    @Test
    @DisplayName("getSummary: 거래처 0건 시 모든 합계 0 + monthlyTrend 6포인트 0")
    fun summaryEmptyAccounts() {
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns emptyList()

        val response = service.getSummary(allScope, 2026, 5, listOf("1000"), null, null)

        assertThat(response.totalTargetAmount).isEqualTo(0L)
        assertThat(response.totalAchievedAmount).isEqualTo(0L)
        assertThat(response.overallAchievementRate).isEqualTo(0.0)
        assertThat(response.totalLastYearAchievedAmount).isNull()
        assertThat(response.monthlyTrend).hasSize(6)
        assertThat(response.monthlyTrend.last().salesYear).isEqualTo(2026)
        assertThat(response.monthlyTrend.last().salesMonth).isEqualTo(5)
    }

    @Test
    @DisplayName("getSummary: 거래처 2건 + 당월 row 2건 → 합계 + 진도율 산출")
    fun summaryWithData() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)

        val rows = listOf(
            history(acc1, target = 1_000_000L, shipSum = 800_000.0),
            history(acc2, target = 500_000.0.toLong(), shipSum = 600_000.0),
        )
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns rows

        val response = service.getSummary(allScope, 2026, 5, listOf("1000"), null, null)

        assertThat(response.totalTargetAmount).isEqualTo(1_500_000L)
        assertThat(response.totalAchievedAmount).isEqualTo(1_400_000L)
        assertThat(response.overallAchievementRate).isCloseTo(93.33, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("getList: 거래처 명세 페이징 + 카테고리 매핑")
    fun listWithPaging() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns listOf(
            history(acc1, target = 1_000_000L, abc1 = 100_000.0, abc2 = 200_000.0, abc3 = 300_000.0, abc4 = 400_000.0),
            history(acc2, target = 500_000L, abc1 = 50_000.0, abc2 = 60_000.0, abc3 = 70_000.0, abc4 = 80_000.0),
        )

        val request = MonthlySalesDashboardListRequest(
            year = 2026, month = 5, costCenterCodes = listOf("1000"),
            page = 0, size = 10,
        )
        val response = service.getList(allScope, request)

        assertThat(response.items).hasSize(2)
        assertThat(response.pageInfo.totalElements).isEqualTo(2L)
        val a = response.items.first { it.accountId == 1 }
        assertThat(a.ambientAchievedAmount).isEqualTo(100_000L)
        assertThat(a.noodleAchievedAmount).isEqualTo(200_000L)
        assertThat(a.frozenRefrigeratedAchievedAmount).isEqualTo(300_000L)
        assertThat(a.oilFatAchievedAmount).isEqualTo(400_000L)
    }

    @Test
    @DisplayName("getList: 정렬 achievementRate,desc")
    fun listSortByRateDesc() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns listOf(
            history(acc1, target = 1_000_000L, shipSum = 500_000.0), // 50%
            history(acc2, target = 500_000L, shipSum = 450_000.0),   // 90%
        )

        val response = service.getList(
            allScope,
            MonthlySalesDashboardListRequest(
                year = 2026, month = 5, costCenterCodes = listOf("1000"),
                sort = "achievementRate,desc",
            )
        )

        assertThat(response.items.first().accountId).isEqualTo(2) // 90% 가 먼저
    }

    @Test
    @DisplayName("applyScope: scope 범위 밖 costCenter → AdminForbiddenException")
    fun scopeForbidden() {
        val scope = branchScope("1010")
        assertThatThrownBy {
            service.getSummary(scope, 2026, 5, listOf("2000"), null, null)
        }.isInstanceOf(AdminForbiddenException::class.java)
    }

    @Test
    @DisplayName("getDetail: 거래처가 권한 범위 밖 → AdminForbiddenException")
    fun detailForbidden() {
        val acc = account(1, "거래처A", "9999")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)

        val scope = branchScope("1000")
        assertThatThrownBy {
            service.getDetail(scope, 1, 2026, 5)
        }.isInstanceOf(AdminForbiddenException::class.java)
    }

    @Test
    @DisplayName("getDetail: happy path + 과거월 카테고리 4종 포함")
    fun detailHappyPath() {
        val acc = account(1, "거래처A", "1000")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        val row = history(acc, year = SalesYear.Y2025, month = SalesMonth.M03, target = 1_000_000L)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2025, SalesMonth.M03, listOf(acc)
            )
        } returns listOf(row)

        val response = service.getDetail(allScope, 1, 2025, 3)

        assertThat(response.customerId).isEqualTo(1)
        assertThat(response.targetAmount).isEqualTo(1_000_000L)
        assertThat(response.categorySales).hasSize(4)
        assertThat(response.categorySales.map { it.category })
            .containsExactly("AMBIENT", "NOODLE", "FROZEN_REFRIGERATED", "OIL_FAT")
    }

    @Test
    @DisplayName("referenceAchievementRate: 과거월 100, 미래월 0, 당월은 영업일 비율")
    fun referenceRate() {
        val pastRate = service.referenceAchievementRate(2024, 5, LocalDate.of(2026, 5, 15))
        assertThat(pastRate).isEqualTo(100.0)

        val futureRate = service.referenceAchievementRate(2030, 5, LocalDate.of(2026, 5, 15))
        assertThat(futureRate).isEqualTo(0.0)

        val sameMonth = service.referenceAchievementRate(2026, 5, LocalDate.of(2026, 5, 15))
        assertThat(sameMonth).isGreaterThan(0.0).isLessThanOrEqualTo(100.0)
    }

    @Test
    @DisplayName("validateParams: 잘못된 month → BusinessException")
    fun invalidMonth() {
        assertThatThrownBy {
            service.getSummary(allScope, 2026, 13, listOf("1000"), null, null)
        }.isInstanceOf(BusinessException::class.java)
    }
}
