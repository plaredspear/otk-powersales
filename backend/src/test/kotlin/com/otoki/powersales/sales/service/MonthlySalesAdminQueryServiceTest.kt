package com.otoki.powersales.sales.service

import com.otoki.orora.entity.OroraMonthlySalesHistory
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

@DisplayName("MonthlySalesAdminQueryService 테스트 — ORORA 직접 조회 hybrid")
class MonthlySalesAdminQueryServiceTest {

    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val ororaGateway: OroraMonthlySalesHistoryQueryGateway = mockk()

    private val service = MonthlySalesAdminQueryService(
        monthlySalesHistoryRepository,
        accountRepository,
        ororaGateway,
    )

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun account(id: Int, name: String, branchCode: String, externalKey: String = "SAP$id"): Account {
        val acc = Account(id = id, sfid = "ACC$id")
        acc.name = name
        acc.branchCode = branchCode
        acc.branchName = "지점$branchCode"
        acc.externalKey = externalKey
        return acc
    }

    /**
     * RDS row helper — `target`, `account`, `confirmed` 등 hybrid 의 RDS 측 필드만 의미 있음.
     * 마감실적 (ship/abc) 은 ORORA row 에서만 사용되므로 본 helper 의 ship/abc 인자는 무시됨.
     */
    private fun history(
        account: Account,
        year: SalesYear = SalesYear.Y2026,
        month: SalesMonth = SalesMonth.M05,
        target: Long? = 1_000_000L,
        confirmed: Boolean? = null,
    ): MonthlySalesHistory {
        val row = MonthlySalesHistory(
            id = (account.id * 100 + (month.value.toInt())).toLong(),
            salesYear = year,
            salesMonth = month,
        )
        row.account = account
        row.thisMonthTarget = target?.let { BigDecimal.valueOf(it) }
        row.isConfirmed = confirmed
        row.sapAccountCode = account.externalKey
        return row
    }

    /**
     * ORORA row helper — `(sapAccountCode, salesDate)` 매칭 키. ABC + Ship 8개 마감실적 필드.
     */
    private fun oroHistory(
        sapAccountCode: String,
        year: Int,
        month: Int,
        ship1: Long = 100_000L,
        ship2: Long = 100_000L,
        ship3: Long = 100_000L,
        ship4: Long = 100_000L,
        abc1: Long = 90_000L,
        abc2: Long = 90_000L,
        abc3: Long = 90_000L,
        abc4: Long = 90_000L,
    ): OroraMonthlySalesHistory = OroraMonthlySalesHistory(
        sapAccountCode = sapAccountCode,
        salesDate = "%04d%02d".format(year, month),
        abcClosingAmount1 = BigDecimal.valueOf(abc1),
        abcClosingAmount2 = BigDecimal.valueOf(abc2),
        abcClosingAmount3 = BigDecimal.valueOf(abc3),
        abcClosingAmount4 = BigDecimal.valueOf(abc4),
        shipClosingAmount1 = BigDecimal.valueOf(ship1),
        shipClosingAmount2 = BigDecimal.valueOf(ship2),
        shipClosingAmount3 = BigDecimal.valueOf(ship3),
        shipClosingAmount4 = BigDecimal.valueOf(ship4),
    )

    @BeforeEach
    fun resetMocks() {
        // 기본 stub — 모든 repo / gateway 호출에 빈 결과 (각 테스트가 필요 시 override)
        every { monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), any()) } returns emptyList()
        every { monthlySalesHistoryRepository.findBySalesYearAndSalesMonthInAndAccountIn(any(), any(), any()) } returns emptyList()
        every { ororaGateway.findBySalesDate(any(), any()) } returns emptyList()
        every { ororaGateway.findBySalesDates(any(), any()) } returns emptyList()
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
    @DisplayName("getSummary: RDS row + ORORA row 결합 → target=RDS, achieved=ORORA shipSum, 진도율 산출")
    fun summaryWithData() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)

        val rdsRows = listOf(
            history(acc1, target = 1_000_000L),
            history(acc2, target = 500_000L),
        )
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns rdsRows

        // ORORA: acc1 ship 합 800k, acc2 ship 합 600k → totalAchieved = 1_400_000
        every {
            ororaGateway.findBySalesDates(listOf("202605", "202505"), listOf("SAP1", "SAP2"))
        } returns listOf(
            oroHistory("SAP1", 2026, 5, ship1 = 200_000L, ship2 = 200_000L, ship3 = 200_000L, ship4 = 200_000L),
            oroHistory("SAP2", 2026, 5, ship1 = 150_000L, ship2 = 150_000L, ship3 = 150_000L, ship4 = 150_000L),
        )

        val response = service.getSummary(allScope, 2026, 5, listOf("1000"), null, null)

        assertThat(response.totalTargetAmount).isEqualTo(1_500_000L)
        assertThat(response.totalAchievedAmount).isEqualTo(1_400_000L)
        assertThat(response.overallAchievementRate).isCloseTo(93.33, org.assertj.core.data.Offset.offset(0.01))
    }

    @Test
    @DisplayName("getSummary: ORORA row 부재 (VPN 장애 시뮬) → achieved=0L, RDS target 유지")
    fun summaryWithOroraFallback() {
        val acc1 = account(1, "거래처A", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1)
            )
        } returns listOf(history(acc1, target = 1_000_000L))
        // ORORA gateway 가 emptyList 반환 (VPN 장애 시뮬) — default stub

        val response = service.getSummary(allScope, 2026, 5, listOf("1000"), null, null)

        assertThat(response.totalTargetAmount).isEqualTo(1_000_000L)
        assertThat(response.totalAchievedAmount).isEqualTo(0L)
        assertThat(response.overallAchievementRate).isEqualTo(0.0)
    }

    @Test
    @DisplayName("getList: 거래처 명세 페이징 + 카테고리별 ABC + Ship 합산")
    fun listWithPaging() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns listOf(
            history(acc1, target = 1_000_000L),
            history(acc2, target = 500_000L),
        )
        every {
            ororaGateway.findBySalesDates(listOf("202605", "202505"), listOf("SAP1", "SAP2"))
        } returns listOf(
            // acc1: AMBIENT = abc1+ship1 = 90_000 + 100_000 = 190_000 등
            oroHistory("SAP1", 2026, 5,
                abc1 = 90_000L, abc2 = 90_000L, abc3 = 90_000L, abc4 = 90_000L,
                ship1 = 100_000L, ship2 = 100_000L, ship3 = 100_000L, ship4 = 100_000L,
            ),
            oroHistory("SAP2", 2026, 5,
                abc1 = 50_000L, abc2 = 60_000L, abc3 = 70_000L, abc4 = 80_000L,
                ship1 = 10_000L, ship2 = 20_000L, ship3 = 30_000L, ship4 = 40_000L,
            ),
        )

        val request = MonthlySalesDashboardListRequest(
            year = 2026, month = 5, costCenterCodes = listOf("1000"),
            page = 0, size = 10,
        )
        val response = service.getList(allScope, request)

        assertThat(response.items).hasSize(2)
        assertThat(response.pageInfo.totalElements).isEqualTo(2L)
        val a = response.items.first { it.accountId == 1 }
        assertThat(a.ambientAchievedAmount).isEqualTo(190_000L)
        assertThat(a.noodleAchievedAmount).isEqualTo(190_000L)
        assertThat(a.frozenRefrigeratedAchievedAmount).isEqualTo(190_000L)
        assertThat(a.oilFatAchievedAmount).isEqualTo(190_000L)
        assertThat(a.totalAchievedAmount).isEqualTo(400_000L) // ship1~4 합

        val b = response.items.first { it.accountId == 2 }
        assertThat(b.ambientAchievedAmount).isEqualTo(60_000L) // abc1+ship1 = 50k+10k
        assertThat(b.noodleAchievedAmount).isEqualTo(80_000L)
        assertThat(b.frozenRefrigeratedAchievedAmount).isEqualTo(100_000L)
        assertThat(b.oilFatAchievedAmount).isEqualTo(120_000L)
    }

    @Test
    @DisplayName("getList: ORORA row 일부 거래처만 적재 → 적재 거래처만 마감실적 출력")
    fun listWithPartialOrora() {
        val acc1 = account(1, "거래처A", "1000")
        val acc2 = account(2, "거래처B", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc1, acc2)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc1, acc2)
            )
        } returns listOf(
            history(acc1, target = 1_000_000L),
            history(acc2, target = 500_000L),
        )
        // ORORA 에 acc1 만 적재됨 (acc2 부재)
        every {
            ororaGateway.findBySalesDates(listOf("202605", "202505"), listOf("SAP1", "SAP2"))
        } returns listOf(
            oroHistory("SAP1", 2026, 5),
        )

        val response = service.getList(
            allScope,
            MonthlySalesDashboardListRequest(year = 2026, month = 5, costCenterCodes = listOf("1000")),
        )

        val a = response.items.first { it.accountId == 1 }
        val b = response.items.first { it.accountId == 2 }
        assertThat(a.totalAchievedAmount).isEqualTo(400_000L) // ORORA 적재
        assertThat(b.totalAchievedAmount).isEqualTo(0L) // ORORA 미적재 → 0
        assertThat(b.ambientAchievedAmount).isEqualTo(0L)
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
            history(acc1, target = 1_000_000L),
            history(acc2, target = 500_000L),
        )
        every {
            ororaGateway.findBySalesDates(listOf("202605", "202505"), listOf("SAP1", "SAP2"))
        } returns listOf(
            // acc1: ship sum = 500_000 → 50%
            oroHistory("SAP1", 2026, 5, ship1 = 125_000L, ship2 = 125_000L, ship3 = 125_000L, ship4 = 125_000L),
            // acc2: ship sum = 450_000 → 90%
            oroHistory("SAP2", 2026, 5, ship1 = 112_500L, ship2 = 112_500L, ship3 = 112_500L, ship4 = 112_500L),
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
    @DisplayName("getDetail: happy path + 과거월 카테고리 4종 포함 (ABC+Ship 합산)")
    fun detailHappyPath() {
        val acc = account(1, "거래처A", "1000")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        val row = history(acc, year = SalesYear.Y2025, month = SalesMonth.M03, target = 1_000_000L)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2025, SalesMonth.M03, listOf(acc)
            )
        } returns listOf(row)
        // ORORA gateway 가 1~3월 + 전년 1~3월 일괄 fetch (sapAccountCodes=[SAP1])
        every {
            ororaGateway.findBySalesDates(any(), listOf("SAP1"))
        } returns listOf(
            oroHistory("SAP1", 2025, 3,
                abc1 = 50_000L, ship1 = 50_000L,
                abc2 = 50_000L, ship2 = 50_000L,
                abc3 = 50_000L, ship3 = 50_000L,
                abc4 = 50_000L, ship4 = 50_000L,
            ),
        )

        val response = service.getDetail(allScope, 1, 2025, 3)

        assertThat(response.customerId).isEqualTo(1)
        assertThat(response.targetAmount).isEqualTo(1_000_000L)
        assertThat(response.categorySales).hasSize(4)
        assertThat(response.categorySales.map { it.category })
            .containsExactly("AMBIENT", "NOODLE", "FROZEN_REFRIGERATED", "OIL_FAT")
        // 카테고리별 achievedAmount = abc + ship = 100_000
        assertThat(response.categorySales.first().achievedAmount).isEqualTo(100_000L)
    }

    @Test
    @DisplayName("categoryAchieved: ORORA ABC + Ship 명시적 합산 (SF Apex IF_REST_MOBILE 가공 로직 정합)")
    fun categoryAchievedAbcPlusShip() {
        // getList 경유로 categoryAchieved 의 합산 정합 검증
        val acc = account(1, "거래처A", "1000")
        every { accountRepository.findByBranchCodeIn(listOf("1000")) } returns listOf(acc)
        every {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountIn(
                SalesYear.Y2026, SalesMonth.M05, listOf(acc)
            )
        } returns listOf(history(acc, target = 1_000_000L))
        every {
            ororaGateway.findBySalesDates(listOf("202605", "202505"), listOf("SAP1"))
        } returns listOf(
            oroHistory("SAP1", 2026, 5,
                abc1 = 100_000L, ship1 = 50_000L,    // AMBIENT = 150_000
                abc2 = 200_000L, ship2 = 30_000L,    // NOODLE = 230_000
                abc3 = 300_000L, ship3 = 20_000L,    // FROZEN_REFRIGERATED = 320_000
                abc4 = 400_000L, ship4 = 10_000L,    // OIL_FAT = 410_000
            ),
        )

        val response = service.getList(
            allScope,
            MonthlySalesDashboardListRequest(year = 2026, month = 5, costCenterCodes = listOf("1000")),
        )
        val item = response.items.first()
        assertThat(item.ambientAchievedAmount).isEqualTo(150_000L)
        assertThat(item.noodleAchievedAmount).isEqualTo(230_000L)
        assertThat(item.frozenRefrigeratedAchievedAmount).isEqualTo(320_000L)
        assertThat(item.oilFatAchievedAmount).isEqualTo(410_000L)
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
