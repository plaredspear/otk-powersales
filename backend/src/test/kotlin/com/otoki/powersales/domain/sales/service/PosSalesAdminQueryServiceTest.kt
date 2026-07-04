package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.LivePosSalesDailyRepository
import com.otoki.pos.repository.PosCustomerSalesRow
import com.otoki.pos.repository.PosSalesRow
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.sales.dto.request.PosSalesAccountListRequest
import com.otoki.powersales.domain.sales.dto.request.PosSalesDashboardListRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("PosSalesAdminQueryService — POS매출 2단 조회 (거래처 조회 + 선택 거래처 POS 집계) 회귀 보호")
class PosSalesAdminQueryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val posRepository: LivePosSalesDailyRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val service = PosSalesAdminQueryService(accountRepository, posRepository, productRepository)

    private val allBranchesScope = DataScope(branchCodes = emptyList(), isAllBranches = true)

    private fun account(
        id: Long,
        externalKey: String?,
        branchCode: String? = "B001",
        distributionChannel: String? = null,
        abcTypeLabel: String? = null,
    ): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
        every { this@mockk.name } returns "거래처$id"
        every { this@mockk.branchCode } returns branchCode
        every { this@mockk.branchName } returns "지점"
        every { this@mockk.distributionChannelLabel() } returns distributionChannel
        every { this@mockk.abcTypeLabel() } returns abcTypeLabel
    }

    private fun customerRow(custCd: String, amt: Long, qty: Long): PosCustomerSalesRow = mockk {
        every { getCustCd() } returns custCd
        every { getSalesAmt() } returns BigDecimal(amt)
        every { getSalesQty() } returns BigDecimal(qty)
    }

    private fun productRow(code: String, name: String, barcode: String?, amt: Long, qty: Long): PosSalesRow = mockk {
        every { getItemCd() } returns code
        every { getItemNm() } returns name
        every { getBarcode() } returns barcode
        every { getSalesAmt() } returns BigDecimal(amt)
        every { getSalesQty() } returns BigDecimal(qty)
    }

    /** 2단 조회 요청 (선택 거래처 id 기반). */
    private fun listRequest(
        accountIds: List<Long> = listOf(1L),
        productIds: List<Long> = emptyList(),
        category2: String? = null,
        category3: String? = null,
        startDate: LocalDate = LocalDate.of(2026, 4, 1),
        endDate: LocalDate = LocalDate.of(2026, 4, 30),
    ) = PosSalesDashboardListRequest(
        startDate = startDate,
        endDate = endDate,
        accountIds = accountIds,
        productIds = productIds,
        category2 = category2,
        category3 = category3,
    )

    /** 1단 거래처 조회 요청. */
    private fun accountRequest(
        customerKeyword: String? = null,
        distributionChannels: List<String> = emptyList(),
        accountTypes: List<String> = emptyList(),
        costCenterCodes: List<String> = listOf("B001"),
    ) = PosSalesAccountListRequest(
        costCenterCodes = costCenterCodes,
        customerKeyword = customerKeyword,
        distributionChannels = distributionChannels,
        accountTypes = accountTypes,
    )

    // ------------------- 1단: getAccounts (거래처 조회, POS 미접촉) -------------------

    @Test
    @DisplayName("getAccounts — 지점 거래처 목록을 POS 조회 없이 반환 (거래처명 오름차순)")
    fun accountsReturnsBranchAccountsWithoutPos() {
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(
            account(2, "S002"),
            account(1, "S001"),
        )

        val result = service.getAccounts(allBranchesScope, accountRequest())

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.items.map { it.accountId }).containsExactly(1L, 2L) // 거래처1, 거래처2 정렬
        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
    }

    @Test
    @DisplayName("getAccounts — 유통형태(distributionChannels) 라벨 필터로 거래처 축소")
    fun accountsFiltersByDistributionChannel() {
        val superMart = account(1, "S001", distributionChannel = "02 슈퍼")
        val cvs = account(2, "S002", distributionChannel = "03 C.V.S")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(superMart, cvs)

        val result = service.getAccounts(allBranchesScope, accountRequest(distributionChannels = listOf("02 슈퍼")))

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().accountId).isEqualTo(1)
    }

    @Test
    @DisplayName("getAccounts — 거래처유형(accountTypes = ABC유형 라벨) 필터로 거래처 축소")
    fun accountsFiltersByAccountType() {
        val emart = account(1, "S001", abcTypeLabel = "6111 이마트")
        val homeplus = account(2, "S002", abcTypeLabel = "6112 홈플러스")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(emart, homeplus)

        val result = service.getAccounts(allBranchesScope, accountRequest(accountTypes = listOf("6112 홈플러스")))

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().accountId).isEqualTo(2)
    }

    @Test
    @DisplayName("getAccounts — 거래처 keyword 필터 (부분일치)")
    fun accountsFiltersByCustomerKeyword() {
        val acc1 = account(1, "S001")
        val acc2 = account(2, "S002")
        every { acc1.name } returns "이마트 원주점"
        every { acc2.name } returns "홈플러스 원주점"
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc1, acc2)

        val result = service.getAccounts(allBranchesScope, accountRequest(customerKeyword = "이마트"))

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().accountId).isEqualTo(1)
    }

    @Test
    @DisplayName("getAccounts — 거래처 수 상한(2500) 초과 시 400 조건 좁힘 안내")
    fun accountsRejectsTooManyAccounts() {
        val accounts = (1..2_501L).map { account(it, "S$it") }
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns accounts

        assertThatThrownBy { service.getAccounts(allBranchesScope, accountRequest()) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("2500건")
    }

    @Test
    @DisplayName("getAccounts — 지점(costCenterCodes) 미지정 시 400")
    fun accountsRejectsEmptyBranches() {
        assertThatThrownBy { service.getAccounts(allBranchesScope, accountRequest(costCenterCodes = emptyList())) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("getAccounts — 권한 범위 밖 지점 요청 시 AdminForbiddenException")
    fun accountsRejectsOutOfScope() {
        val scope = DataScope(branchCodes = listOf("B999"), isAllBranches = false)

        assertThatThrownBy { service.getAccounts(scope, accountRequest()) }
            .isInstanceOf(AdminForbiddenException::class.java)
    }

    // ------------------- 2단: getList (선택 거래처 POS 집계) -------------------

    @Test
    @DisplayName("getList — 선택 거래처 POS 합계가 custCd(000+externalKey) 로 거래처에 결합")
    fun listJoinsPosByCustCd() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
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
    @DisplayName("getList — 응답에 선택 거래처 전체(페이징 무관) POS매출 금액/수량 합계 포함")
    fun listIncludesGrandTotals() {
        val accounts = (1..3L).map { account(it, "S00$it") }
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L, 2L, 3L), true) } returns accounts
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns listOf(
            customerRow("000S001", amt = 1000, qty = 1),
            customerRow("000S002", amt = 2000, qty = 2),
            customerRow("000S003", amt = 3000, qty = 3),
        )

        val result = service.getList(allBranchesScope, listRequest(accountIds = listOf(1L, 2L, 3L)).copy(size = 2))

        assertThat(result.items).hasSize(2) // 페이징 적용
        assertThat(result.totalSalesAmount).isEqualTo(6000L) // 합계는 선택 거래처 전체 기준
        assertThat(result.totalSalesQuantity).isEqualTo(6L)
    }

    @Test
    @DisplayName("getList — 선택 거래처는 단일 청크(≤20)라 POS CUST_CD IN 왕복 1회")
    fun listAggregatesSelectedInSingleCall() {
        val accounts = (1..20L).map { account(it, "S%04d".format(it)) }
        val ids = accounts.map { it.id }
        every { accountRepository.findByIdInAndIsDeletedNot(ids, true) } returns accounts
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        service.getList(allBranchesScope, listRequest(accountIds = ids).copy(size = 100))

        verify(exactly = 1) { posRepository.aggregateByCustomer(any(), any(), any()) }
    }

    @Test
    @DisplayName("getList — 제품 필터 시 바코드 해소 후 BARCODE IN 집계 분기 사용")
    fun listUsesBarcodeAggregationWhenProductFilter() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        every {
            productRepository.findBarcodesForElectronicSales(listOf(10L), null, null)
        } returns listOf("880001", "880002")
        every {
            posRepository.aggregateByCustomerAndBarcodes(listOf("000S001"), any(), any(), listOf("880001", "880002"))
        } returns listOf(customerRow("000S001", amt = 700, qty = 7))

        val result = service.getList(allBranchesScope, listRequest(productIds = listOf(10L)))

        assertThat(result.items.first().salesAmount).isEqualTo(700L)
        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
    }

    @Test
    @DisplayName("getList — 바코드가 청크 크기(1000) 초과 시 분할 호출 + custCd 합산 병합")
    fun listChunksBarcodes() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        val barcodes = (1..1500).map { "BC$it" }
        every {
            productRepository.findBarcodesForElectronicSales(emptyList(), "면류", null)
        } returns barcodes
        every {
            posRepository.aggregateByCustomerAndBarcodes(any(), any(), any(), barcodes.take(1000))
        } returns listOf(customerRow("000S001", amt = 1000, qty = 10))
        every {
            posRepository.aggregateByCustomerAndBarcodes(any(), any(), any(), barcodes.drop(1000))
        } returns listOf(customerRow("000S001", amt = 500, qty = 5))

        val result = service.getList(allBranchesScope, listRequest(category2 = "면류"))

        assertThat(result.items.first().salesAmount).isEqualTo(1500L)
        assertThat(result.items.first().salesQuantity).isEqualTo(15L)
        verify(exactly = 2) { posRepository.aggregateByCustomerAndBarcodes(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("getList — 선택 거래처 수 상한(20) 초과 시 POS 미조회 + 400")
    fun listRejectsTooManySelectedAccounts() {
        val ids = (1..21L).toList()

        assertThatThrownBy { service.getList(allBranchesScope, listRequest(accountIds = ids)) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("20개")

        verify(exactly = 0) { accountRepository.findByIdInAndIsDeletedNot(any(), any()) }
        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
    }

    @Test
    @DisplayName("getList — 선택 거래처 수 상한(20) 경계는 허용")
    fun listAllowsSelectedAtLimit() {
        val accounts = (1..20L).map { account(it, "S%04d".format(it)) }
        val ids = accounts.map { it.id }
        every { accountRepository.findByIdInAndIsDeletedNot(ids, true) } returns accounts
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest(accountIds = ids).copy(size = 100))

        assertThat(result.pageInfo.totalElements).isEqualTo(20L)
    }

    @Test
    @DisplayName("getList — 선택 거래처 미지정(빈 목록) 시 400")
    fun listRejectsEmptySelection() {
        assertThatThrownBy { service.getList(allBranchesScope, listRequest(accountIds = emptyList())) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("getList — 제품 필터 지정 + 매칭 바코드 0건이면 POS 미호출 + 전 거래처 0")
    fun listSkipsPosWhenNoBarcodeMatches() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        every {
            productRepository.findBarcodesForElectronicSales(emptyList(), "면류", "봉지면")
        } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest(category2 = "면류", category3 = "봉지면"))

        assertThat(result.items.first().salesAmount).isEqualTo(0L)
        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
        verify(exactly = 0) { posRepository.aggregateByCustomerAndBarcodes(any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("getList — POS 도달 불가(예외) 시 graceful fallback 으로 0/0")
    fun listGracefulFallbackOnPosError() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        every {
            posRepository.aggregateByCustomer(any(), any(), any())
        } throws RuntimeException("POS 도달 불가")

        val result = service.getList(allBranchesScope, listRequest())

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().salesAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getList — 시작일이 종료일보다 이후면 400")
    fun listRejectsReversedRange() {
        assertThatThrownBy {
            service.getList(
                allBranchesScope,
                listRequest(startDate = LocalDate.of(2026, 5, 2), endDate = LocalDate.of(2026, 5, 1)),
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("getList — 31일(두 끝점 일수 차이) 경계는 허용, 초과는 400 — 레거시 maxSpan 정합")
    fun listEnforcesMaxRangeDays() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        // 2026-01-01 ~ 2026-02-01 = 일수 차이 31 → 허용
        service.getList(
            allBranchesScope,
            listRequest(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 2, 1)),
        )

        // 하루 초과 (일수 차이 32) → 400
        assertThatThrownBy {
            service.getList(
                allBranchesScope,
                listRequest(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 2, 2)),
            )
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    @DisplayName("getList — 선택 거래처 중 권한 범위 밖이 있으면 AdminForbiddenException")
    fun listRejectsOutOfScope() {
        val acc = account(1, "S001", branchCode = "B001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1L), true) } returns listOf(acc)
        val scope = DataScope(branchCodes = listOf("B999"), isAllBranches = false)

        assertThatThrownBy { service.getList(scope, listRequest()) }
            .isInstanceOf(AdminForbiddenException::class.java)
    }

    @Test
    @DisplayName("getListForExport — 선택 거래처 수 상한(20) 이 export 경로에도 동일 적용")
    fun exportRejectsTooManySelectedAccounts() {
        val ids = (1..21L).toList()

        assertThatThrownBy { service.getListForExport(allBranchesScope, listRequest(accountIds = ids)) }
            .isInstanceOf(BusinessException::class.java)

        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
    }

    // ------------------- getDetail (기존 유지) -------------------

    @Test
    @DisplayName("getDetail — 제품별 명세(바코드 포함) + 합계 산출")
    fun detailAggregatesProducts() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every {
            posRepository.aggregateByProduct("000S001", "2026-04-01", "2026-04-30")
        } returns listOf(
            productRow("P1", "라면", "880001", amt = 3000, qty = 10),
            productRow("P2", "케첩", "880002", amt = 2000, qty = 5),
        )

        val result = service.getDetail(
            allBranchesScope,
            customerId = 1,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 30),
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.first().barcode).isEqualTo("880001")
        assertThat(result.totalAmount).isEqualTo(5000L)
        assertThat(result.totalQuantity).isEqualTo(15L)
    }

    @Test
    @DisplayName("getDetail — 제품 필터 시 청크 결과를 제품코드 단위로 병합 (바코드는 첫 non-null 유지)")
    fun detailMergesChunkRowsByItemCd() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        val barcodes = (1..1500).map { "BC$it" }
        every {
            productRepository.findBarcodesForElectronicSales(listOf(10L), null, null)
        } returns barcodes
        every {
            posRepository.aggregateByProductAndBarcodes("000S001", any(), any(), barcodes.take(1000))
        } returns listOf(productRow("P1", "라면", null, amt = 300, qty = 3))
        every {
            posRepository.aggregateByProductAndBarcodes("000S001", any(), any(), barcodes.drop(1000))
        } returns listOf(productRow("P1", "라면", "880002", amt = 200, qty = 2))

        val result = service.getDetail(
            allBranchesScope,
            customerId = 1,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 30),
            productIds = listOf(10L),
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().productCode).isEqualTo("P1")
        assertThat(result.items.first().barcode).isEqualTo("880002")
        assertThat(result.items.first().amount).isEqualTo(500L)
        assertThat(result.totalQuantity).isEqualTo(5L)
    }

    @Test
    @DisplayName("getDetail — externalKey null → POS 조회 생략, 빈 명세")
    fun detailHandlesNullExternalKey() {
        val acc = account(1, externalKey = null)
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)

        val result = service.getDetail(
            allBranchesScope,
            customerId = 1,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 30),
        )

        assertThat(result.items).isEmpty()
        assertThat(result.totalAmount).isEqualTo(0L)
    }

    @Test
    @DisplayName("getDetail — 권한 범위 밖 거래처는 AdminForbiddenException")
    fun detailRejectsOutOfScope() {
        val acc = account(1, "S001", branchCode = "B001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        val scope = DataScope(branchCodes = listOf("B999"), isAllBranches = false)

        assertThatThrownBy {
            service.getDetail(
                scope,
                customerId = 1,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 4, 30),
            )
        }.isInstanceOf(AdminForbiddenException::class.java)
    }
}
