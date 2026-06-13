package com.otoki.powersales.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
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
    private val service = MonthlySalesAdminQueryService(accountRepository, monthlySalesHistoryGateway)

    private val allBranchesScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    private fun account(id: Long, externalKey: String?, branchCode: String? = "B001"): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
        every { this@mockk.name } returns "거래처$id"
        every { this@mockk.branchCode } returns branchCode
        every { this@mockk.branchName } returns "지점"
    }

    private fun row(
        sapCode: String,
        salesDate: String,
        ship1: Long = 0L,
        ship2: Long = 0L,
        ship3: Long = 0L,
        ship4: Long = 0L,
    ) = MonthlySalesRow(
        sapAccountCode = sapCode,
        salesDate = salesDate,
        closingAmountSum = BigDecimal(ship1 + ship2 + ship3 + ship4),
        abcClosingAmount1 = null,
        shipClosingAmount1 = BigDecimal(ship1),
        shipClosingAmount2 = BigDecimal(ship2),
        shipClosingAmount3 = BigDecimal(ship3),
        shipClosingAmount4 = BigDecimal(ship4),
    )

    @Test
    @DisplayName("getDetail — Ship 4종 합산 = totalAchievedAmount + target/rate 폐기 (0/0.0)")
    fun detailSumsShipFourCategories() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns listOf(
            row("S001", "202604", ship1 = 100, ship2 = 200, ship3 = 300, ship4 = 400),
        )

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(1000L)
        assertThat(result.targetAmount).isEqualTo(0L)
        assertThat(result.achievementRate).isEqualTo(0.0)
    }

    @Test
    @DisplayName("getDetail — RDS row 부재 → achievedAmount = 0")
    fun detailReturnsZeroWhenNoOroraRow() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDates(any(), any()) } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getDetail — Account.externalKey null → gateway 호출 시 빈 리스트 전달")
    fun detailHandlesNullExternalKey() {
        val acc = account(1, externalKey = null)
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every { monthlySalesHistoryGateway.findBySalesDates(any(), emptyList()) } returns emptyList()

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.achievedAmount).isEqualTo(0L)
    }
}
