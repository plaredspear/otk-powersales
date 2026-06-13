package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.ElectronicSalesCustomerRow
import com.otoki.pos.repository.ElectronicSalesRow
import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.sales.service.ElectronicSalesAdminQueryService
import com.otoki.powersales.domain.sales.dto.request.ElectronicSalesDashboardListRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("ElectronicSalesAdminQueryService — POS 기반 전산실적 응답 회귀 보호")
class ElectronicSalesAdminQueryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val posRepository: LiveTotSalesDailyRepository = mockk()
    private val service = ElectronicSalesAdminQueryService(accountRepository, posRepository)

    private val allBranchesScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    private fun account(id: Long, externalKey: String?, branchCode: String? = "B001"): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
        every { this@mockk.name } returns "거래처$id"
        every { this@mockk.branchCode } returns branchCode
        every { this@mockk.branchName } returns "지점"
    }

    private fun customerRow(custCd: String, amt: Long, qty: Long): ElectronicSalesCustomerRow = mockk {
        every { getCustCd() } returns custCd
        every { getSalesAmt() } returns BigDecimal(amt)
        every { getSalesQty() } returns BigDecimal(qty)
    }

    private fun productRow(code: String, name: String, amt: Long, qty: Long): ElectronicSalesRow = mockk {
        every { getItemCd() } returns code
        every { getItemNm() } returns name
        every { getSalesAmt() } returns BigDecimal(amt)
        every { getSalesQty() } returns BigDecimal(qty)
    }

    private fun listRequest(accountGroup: String? = null, customerKeyword: String? = null) =
        ElectronicSalesDashboardListRequest(
            year = 2026,
            month = 4,
            costCenterCodes = listOf("B001"),
            accountGroup = accountGroup,
            customerKeyword = customerKeyword,
        )

    @Test
    @DisplayName("getList — POS 거래처별 합계가 custCd(000+externalKey) 로 거래처에 결합")
    fun listJoinsPosByCustCd() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every {
            posRepository.aggregateByCustomer(listOf("000S001"), "2026-04-01", "2026-04-30")
        } returns listOf(customerRow("000S001", amt = 5000, qty = 12))

        val result = service.getList(allBranchesScope, listRequest())

        assertThat(result.items).hasSize(1)
        with(result.items.first()) {
            assertThat(accountId).isEqualTo(1)
            assertThat(salesAmount).isEqualTo(5000L)
            assertThat(salesQuantity).isEqualTo(12L)
        }
    }

    @Test
    @DisplayName("getList — POS 매칭 없는 거래처는 0/0")
    fun listReturnsZeroWhenNoPosRow() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest())

        assertThat(result.items.first().salesAmount).isEqualTo(0L)
        assertThat(result.items.first().salesQuantity).isEqualTo(0L)
    }

    @Test
    @DisplayName("getList — POS 도달 불가(예외) 시 graceful fallback 으로 0/0")
    fun listGracefulFallbackOnPosError() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every {
            posRepository.aggregateByCustomer(any(), any(), any())
        } throws RuntimeException("POS 도달 불가")

        val result = service.getList(allBranchesScope, listRequest())

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().salesAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getDetail — 제품별 명세 + 합계 산출")
    fun detailAggregatesProducts() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every {
            posRepository.aggregateByProduct("000S001", "2026-04-01", "2026-04-30")
        } returns listOf(
            productRow("P1", "라면", amt = 3000, qty = 10),
            productRow("P2", "케첩", amt = 2000, qty = 5),
        )

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.items).hasSize(2)
        assertThat(result.totalAmount).isEqualTo(5000L)
        assertThat(result.totalQuantity).isEqualTo(15L)
    }

    @Test
    @DisplayName("getDetail — externalKey null → POS 조회 생략, 빈 명세")
    fun detailHandlesNullExternalKey() {
        val acc = account(1, externalKey = null)
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)

        val result = service.getDetail(allBranchesScope, customerId = 1, year = 2026, month = 4)

        assertThat(result.items).isEmpty()
        assertThat(result.totalAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getList — 권한 범위 밖 지점 요청 시 AdminForbiddenException")
    fun listRejectsOutOfScope() {
        val scope = DataScope(branchCodes = listOf("B999"), isAllBranches = false)

        assertThatThrownBy { service.getList(scope, listRequest()) }
            .isInstanceOf(AdminForbiddenException::class.java)
    }
}
