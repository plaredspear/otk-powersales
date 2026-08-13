package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.policy.ClosedAccountSalesExemption
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.sales.service.MonthlySalesRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [ClosedAccountSalesExemptionResolver] — 폐업 거래처의 최근 매출 예외 대상 판별.
 *
 * 진열사원스케줄 등록 검증([com.otoki.powersales.domain.activity.schedule.service.ScheduleUploadValidator])
 * 이 거래처 lookup 과 동일한 기준으로 폐업을 차단하도록, 면제 대상 id 집합을 산출한다.
 */
@DisplayName("ClosedAccountSalesExemptionResolver — 폐업 거래처 최근 매출 예외 판별")
class ClosedAccountSalesExemptionResolverTest {

    private val gateway: MonthlySalesHistoryQueryGateway = mockk()
    private val resolver = ClosedAccountSalesExemptionResolver(gateway)

    private fun account(
        id: Long,
        status: String?,
        distribution: String? = null,
        abcTypeCode: String? = null,
    ) = Account(
        id = id,
        externalKey = "EXT-$id",
        name = "거래처$id",
        accountStatusName = status,
        distribution = distribution,
        abcTypeCode = abcTypeCode,
    )

    private fun salesRow(accountId: Long, amount: String) = MonthlySalesRow(
        sapAccountCode = "SAP-$accountId",
        salesDate = "202608",
        closingAmountSum = BigDecimal(amount),
        accountId = accountId,
        abcClosingAmount1 = null,
    )

    @Test
    @DisplayName("폐업 + 매출(>0) 보유 거래처만 면제 대상")
    fun exemptsClosedAccountWithPositiveSales() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(salesRow(1L, "1000000"))

        assertThat(resolver.resolveExemptedAccountIds(listOf(closed))).containsExactly(1L)
    }

    @Test
    @DisplayName("매출 0 은 면제 대상 아님")
    fun doesNotExemptOnZeroSales() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(salesRow(1L, "0"))

        assertThat(resolver.resolveExemptedAccountIds(listOf(closed))).isEmpty()
    }

    @Test
    @DisplayName("매출 음수(반품·조정) 는 면제 대상 아님")
    fun doesNotExemptOnNegativeSales() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(salesRow(1L, "-500000"))

        assertThat(resolver.resolveExemptedAccountIds(listOf(closed))).isEmpty()
    }

    @Test
    @DisplayName("매출 row 자체가 없으면 면제 대상 아님")
    fun doesNotExemptWithoutSalesRow() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()

        assertThat(resolver.resolveExemptedAccountIds(listOf(closed))).isEmpty()
    }

    @Test
    @DisplayName("비폐업 거래처만 있으면 매출 조회 자체를 하지 않음")
    fun skipsSalesQueryWhenNoClosedAccount() {
        val active = account(1L, "거래")

        assertThat(resolver.resolveExemptedAccountIds(listOf(active))).isEmpty()
        verify(exactly = 0) { gateway.findBySalesDatesByAccountId(any(), any()) }
    }

    @Test
    @DisplayName("조회 모수는 폐업 거래처로 한정 — 비폐업은 IN 절에 포함하지 않음")
    fun queriesOnlyClosedAccounts() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        val active = account(2L, "거래")
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()

        resolver.resolveExemptedAccountIds(listOf(closed, active))

        verify { gateway.findBySalesDatesByAccountId(any(), setOf(1L)) }
    }

    @Test
    @DisplayName("SF 원본 면제(distribution / 3062) 대상은 매출 조회 모수에서 제외 — 호출 측이 OR 로 통과시킴")
    fun excludesAttributeExemptAccountsFromQuery() {
        val closedPlain = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        val closedWithDistribution = account(2L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED, distribution = "X")
        val closedWith3062 = account(
            3L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED,
            abcTypeCode = ClosedAccountSalesExemption.ABC_TYPE_CODE_EXEMPT
        )
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()

        resolver.resolveExemptedAccountIds(listOf(closedPlain, closedWithDistribution, closedWith3062))

        verify { gateway.findBySalesDatesByAccountId(any(), setOf(1L)) }
    }

    @Test
    @DisplayName("SF 원본 면제 대상만 있으면 매출 조회 자체를 하지 않음")
    fun skipsQueryWhenAllClosedAreAttributeExempt() {
        val closedWithDistribution = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED, distribution = "X")

        assertThat(resolver.resolveExemptedAccountIds(listOf(closedWithDistribution))).isEmpty()
        verify(exactly = 0) { gateway.findBySalesDatesByAccountId(any(), any()) }
    }

    @Test
    @DisplayName("조회 매출월은 당월·전월 2개 (YYYYMM)")
    fun queriesCurrentAndPreviousMonth() {
        val closed = account(1L, ClosedAccountSalesExemption.ACCOUNT_STATUS_CLOSED)
        every { gateway.findBySalesDatesByAccountId(any(), any()) } returns emptyList()

        resolver.resolveExemptedAccountIds(listOf(closed))

        val today = LocalDate.now()
        val expected = listOf(today, today.minusMonths(1))
            .map { "%04d%02d".format(it.year, it.monthValue) }
        verify { gateway.findBySalesDatesByAccountId(expected, any()) }
    }

    @Test
    @DisplayName("빈 입력이면 매출 조회 없이 빈 집합")
    fun returnsEmptyOnEmptyInput() {
        assertThat(resolver.resolveExemptedAccountIds(emptyList())).isEmpty()
        verify(exactly = 0) { gateway.findBySalesDatesByAccountId(any(), any()) }
    }
}
