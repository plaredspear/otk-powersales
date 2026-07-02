package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.ElectronicSalesCustomerRow
import com.otoki.pos.repository.ElectronicSalesProductRow
import com.otoki.pos.repository.ElectronicSalesRow
import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountLabelPartsRow
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.repository.CategoryGroupRow
import com.otoki.powersales.domain.foundation.product.repository.ElectronicSalesProductLookupRow
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.sales.dto.request.ElectronicSalesDashboardListRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("ElectronicSalesAdminQueryService — POS 기반 전산실적 응답 회귀 보호")
class ElectronicSalesAdminQueryServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val posRepository: LiveTotSalesDailyRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val service = ElectronicSalesAdminQueryService(accountRepository, posRepository, productRepository)

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

    private fun productBarcodeRow(code: String, name: String, barcode: String, amt: Long, qty: Long): ElectronicSalesProductRow = mockk {
        every { getItemCd() } returns code
        every { getItemNm() } returns name
        every { getBarcode() } returns barcode
        every { getSalesAmt() } returns BigDecimal(amt)
        every { getSalesQty() } returns BigDecimal(qty)
    }

    private fun listRequest(
        accountGroup: String? = null,
        customerKeyword: String? = null,
        distributionChannels: List<String> = emptyList(),
        accountTypes: List<String> = emptyList(),
        productIds: List<Long> = emptyList(),
        category2: String? = null,
        category3: String? = null,
        startDate: LocalDate = LocalDate.of(2026, 4, 1),
        endDate: LocalDate = LocalDate.of(2026, 4, 30),
    ) = ElectronicSalesDashboardListRequest(
        startDate = startDate,
        endDate = endDate,
        costCenterCodes = listOf("B001"),
        accountGroup = accountGroup,
        customerKeyword = customerKeyword,
        distributionChannels = distributionChannels,
        accountTypes = accountTypes,
        productIds = productIds,
        category2 = category2,
        category3 = category3,
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
    @DisplayName("getList — 일 단위 기간(startDate/endDate)이 그대로 POS 조회 범위로 전달")
    fun listPassesDayRange() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every {
            posRepository.aggregateByCustomer(listOf("000S001"), "2026-04-05", "2026-05-20")
        } returns emptyList()

        service.getList(
            allBranchesScope,
            listRequest(startDate = LocalDate.of(2026, 4, 5), endDate = LocalDate.of(2026, 5, 20)),
        )

        verify(exactly = 1) {
            posRepository.aggregateByCustomer(listOf("000S001"), "2026-04-05", "2026-05-20")
        }
    }

    @Test
    @DisplayName("getList — 응답에 전체(페이징 무관) 전산매출 금액/수량 합계 포함")
    fun listIncludesGrandTotals() {
        val accounts = (1..3L).map { account(it, "S00$it") }
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns accounts
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns listOf(
            customerRow("000S001", amt = 1000, qty = 1),
            customerRow("000S002", amt = 2000, qty = 2),
            customerRow("000S003", amt = 3000, qty = 3),
        )

        val result = service.getList(allBranchesScope, listRequest().copy(size = 2))

        assertThat(result.items).hasSize(2) // 페이징 적용
        assertThat(result.totalSalesAmount).isEqualTo(6000L) // 합계는 전체 기준
        assertThat(result.totalSalesQuantity).isEqualTo(6L)
    }

    @Test
    @DisplayName("getList — 유통형태(distributionChannels) 라벨 필터로 거래처 축소")
    fun listFiltersByDistributionChannel() {
        val superMart = account(1, "S001", distributionChannel = "02 슈퍼")
        val cvs = account(2, "S002", distributionChannel = "03 C.V.S")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(superMart, cvs)
        every { posRepository.aggregateByCustomer(listOf("000S001"), any(), any()) } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest(distributionChannels = listOf("02 슈퍼")))

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().accountId).isEqualTo(1)
    }

    @Test
    @DisplayName("getList — 거래처유형(accountTypes = ABC유형 라벨) 필터로 거래처 축소")
    fun listFiltersByAccountType() {
        val emart = account(1, "S001", abcTypeLabel = "6111 이마트")
        val homeplus = account(2, "S002", abcTypeLabel = "6112 홈플러스")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(emart, homeplus)
        every { posRepository.aggregateByCustomer(listOf("000S002"), any(), any()) } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest(accountTypes = listOf("6112 홈플러스")))

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().accountId).isEqualTo(2)
    }

    @Test
    @DisplayName("getList — 제품 필터 시 바코드 해소 후 UPC_CD IN 집계 분기 사용")
    fun listUsesBarcodeAggregationWhenProductFilter() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
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
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
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
    @DisplayName("getList — 제품 필터 지정 + 매칭 바코드 0건이면 POS 미호출 + 전 거래처 0")
    fun listSkipsPosWhenNoBarcodeMatches() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every {
            productRepository.findBarcodesForElectronicSales(emptyList(), "면류", "봉지면")
        } returns emptyList()

        val result = service.getList(allBranchesScope, listRequest(category2 = "면류", category3 = "봉지면"))

        assertThat(result.items.first().salesAmount).isEqualTo(0L)
        verify(exactly = 0) { posRepository.aggregateByCustomer(any(), any(), any()) }
        verify(exactly = 0) { posRepository.aggregateByCustomerAndBarcodes(any(), any(), any(), any()) }
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
    @DisplayName("getList — 3개월(포함) 경계는 허용, 초과는 400")
    fun listEnforcesMaxRangeMonths() {
        val acc = account(1, "S001")
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns listOf(acc)
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        // 2026-01-01 ~ 2026-03-31 = 포함 3개월 → 허용
        service.getList(
            allBranchesScope,
            listRequest(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 3, 31)),
        )

        // 하루 초과 → 400
        assertThatThrownBy {
            service.getList(
                allBranchesScope,
                listRequest(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 4, 1)),
            )
        }.isInstanceOf(BusinessException::class.java)
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

        val result = service.getDetail(
            allBranchesScope,
            customerId = 1,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 30),
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.totalAmount).isEqualTo(5000L)
        assertThat(result.totalQuantity).isEqualTo(15L)
    }

    @Test
    @DisplayName("getDetail — 제품 필터 시 UPC_CD 행을 제품코드 단위로 병합")
    fun detailMergesBarcodeRowsByItemCd() {
        val acc = account(1, "S001")
        every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(acc)
        every {
            productRepository.findBarcodesForElectronicSales(listOf(10L), null, null)
        } returns listOf("880001", "880002")
        every {
            posRepository.aggregateByProductBarcodes("000S001", any(), any(), listOf("880001", "880002"))
        } returns listOf(
            productBarcodeRow("P1", "라면", "880001", amt = 300, qty = 3),
            productBarcodeRow("P1", "라면", "880002", amt = 200, qty = 2),
        )

        val result = service.getDetail(
            allBranchesScope,
            customerId = 1,
            startDate = LocalDate.of(2026, 4, 1),
            endDate = LocalDate.of(2026, 4, 30),
            productIds = listOf(10L),
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().productCode).isEqualTo("P1")
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
    @DisplayName("getList — 권한 범위 밖 지점 요청 시 AdminForbiddenException")
    fun listRejectsOutOfScope() {
        val scope = DataScope(branchCodes = listOf("B999"), isAllBranches = false)

        assertThatThrownBy { service.getList(scope, listRequest()) }
            .isInstanceOf(AdminForbiddenException::class.java)
    }

    @Test
    @DisplayName("getListForExport — 결과가 EXPORT_MAX_ROWS(50000) 로 절단된다")
    fun exportCapsAtMaxRows() {
        val accounts = (1..50_001L).map { account(it, "S$it") }
        every { accountRepository.findByBranchCodeIn(listOf("B001")) } returns accounts
        every { posRepository.aggregateByCustomer(any(), any(), any()) } returns emptyList()

        val result = service.getListForExport(allBranchesScope, listRequest())

        assertThat(result).hasSize(50_000)
    }

    @Test
    @DisplayName("getFilterOptions — 유통형태/거래처유형 라벨 조합 + 중분류→소분류 트리")
    fun filterOptionsCombinesLabelsAndCategories() {
        every { accountRepository.findDistinctDistributionChannelParts() } returns listOf(
            AccountLabelPartsRow("02", "슈퍼"),
            AccountLabelPartsRow(null, null), // 라벨 조합 불가 → 제외
        )
        every { accountRepository.findDistinctAbcTypeParts() } returns listOf(
            AccountLabelPartsRow("6111", "이마트"),
        )
        every { productRepository.findCategoryGroups() } returns listOf(
            CategoryGroupRow("면류", "봉지면"),
            CategoryGroupRow("면류", "용기면"),
            CategoryGroupRow("면류", null), // 소분류 미지정 제품 → 소분류 목록에서 제외
            CategoryGroupRow("소스류", "케첩"),
        )

        val result = service.getFilterOptions()

        assertThat(result.distributionChannels).containsExactly("02 슈퍼")
        assertThat(result.accountTypes).containsExactly("6111 이마트")
        assertThat(result.categories).hasSize(2)
        val noodle = result.categories.first { it.category2 == "면류" }
        assertThat(noodle.category3s).containsExactly("봉지면", "용기면")
    }

    @Test
    @DisplayName("searchProducts — keyword 공백이면 빈 결과, 아니면 lookup 결과 매핑")
    fun searchProductsMapsLookup() {
        assertThat(service.searchProducts("   ")).isEmpty()

        every { productRepository.searchForElectronicSales("진라면", 50L) } returns listOf(
            ElectronicSalesProductLookupRow(productId = 10, name = "진라면", productCode = "P100", barcode = "880001"),
        )

        val result = service.searchProducts(" 진라면 ")

        assertThat(result).hasSize(1)
        assertThat(result.first().productId).isEqualTo(10L)
        assertThat(result.first().barcode).isEqualTo("880001")
    }
}
