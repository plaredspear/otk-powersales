package com.otoki.powersales.sales.service

import com.otoki.pos.repository.ElectronicSalesCustomerRow
import com.otoki.pos.repository.ElectronicSalesProductRow
import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("ElectronicSalesService 테스트")
class ElectronicSalesServiceTest {

	private val accountRepository: AccountRepository = mockk()
	private val liveTotSalesDailyRepository: LiveTotSalesDailyRepository = mockk()

	private val service = ElectronicSalesService(accountRepository, liveTotSalesDailyRepository)

	private fun account(id: Long = 1L, name: String = "사과마을", externalKey: String? = "12345") =
		Account(id = id, name = name, externalKey = externalKey)

	private fun productRow(
		itemCd: String,
		itemNm: String?,
		barcode: String?,
		amt: Long,
		qty: Long,
	): ElectronicSalesProductRow {
		val r = mockk<ElectronicSalesProductRow>()
		every { r.getItemCd() } returns itemCd
		every { r.getItemNm() } returns itemNm
		every { r.getBarcode() } returns barcode
		every { r.getSalesAmt() } returns BigDecimal.valueOf(amt)
		every { r.getSalesQty() } returns BigDecimal.valueOf(qty)
		return r
	}

	private fun customerRow(custCd: String, amt: Long, qty: Long): ElectronicSalesCustomerRow {
		val r = mockk<ElectronicSalesCustomerRow>()
		every { r.getCustCd() } returns custCd
		every { r.getSalesAmt() } returns BigDecimal.valueOf(amt)
		every { r.getSalesQty() } returns BigDecimal.valueOf(qty)
		return r
	}

	@Test
	@DisplayName("매출 조회 제품(바코드) 선택 시 '000' 패딩 CUST_CD + 기간 + UPC_CD IN 으로 제품별 조회·합계")
	fun querySelectedProducts() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = "12345"))
		val custSlot = slot<String>()
		val startSlot = slot<String>()
		val endSlot = slot<String>()
		val barcodeSlot = slot<List<String>>()
		every {
			liveTotSalesDailyRepository.aggregateByProductBarcodes(
				capture(custSlot), capture(startSlot), capture(endSlot), capture(barcodeSlot),
			)
		} returns listOf(
			productRow("01101123", "갈릭 아이올리소스 240g", "8801234500011", 3500, 10),
			productRow("01101222", "오뚜기 3분 카레 100g", "8801234500028", 1500, 5),
		)

		val res = service.getElectronicSales(
			customerId = 1,
			startDate = "2026-06-01",
			endDate = "2026-06-09",
			barcodes = listOf("8801234500011", "8801234500028"),
		)

		// 레거시 CUST_CD = "000" + accountCode, 기간은 그대로 전달
		assertThat(custSlot.captured).isEqualTo("00012345")
		assertThat(startSlot.captured).isEqualTo("2026-06-01")
		assertThat(endSlot.captured).isEqualTo("2026-06-09")
		assertThat(barcodeSlot.captured).containsExactly("8801234500011", "8801234500028")

		assertThat(res.customerId).isEqualTo(1)
		assertThat(res.sapAccountCode).isEqualTo("12345")
		assertThat(res.startDate).isEqualTo("2026-06-01")
		assertThat(res.endDate).isEqualTo("2026-06-09")
		// 합계금액 = 선택 제품 금액 합
		assertThat(res.totalAmount).isEqualTo(5000L)
		assertThat(res.items).hasSize(2)
		assertThat(res.items[0].productCode).isEqualTo("01101123")
		assertThat(res.items[0].productName).isEqualTo("갈릭 아이올리소스 240g")
		assertThat(res.items[0].barcode).isEqualTo("8801234500011")
		assertThat(res.items[0].amount).isEqualTo(3500L)
		assertThat(res.items[0].quantity).isEqualTo(10L)
	}

	@Test
	@DisplayName("매출 조회 제품 미선택 시 제품 명세 없이 거래처·기간 전체 합계금액만 (레거시 abcSumAmount)")
	fun querySumOnly() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = "12345"))
		every {
			liveTotSalesDailyRepository.aggregateByCustomer(listOf("00012345"), "2026-06-01", "2026-06-09")
		} returns listOf(customerRow("00012345", 123_456L, 99L))

		val res = service.getElectronicSales(
			customerId = 1,
			startDate = "2026-06-01",
			endDate = "2026-06-09",
			barcodes = null,
		)

		assertThat(res.items).isEmpty()
		assertThat(res.totalAmount).isEqualTo(123_456L)
		verify(exactly = 0) { liveTotSalesDailyRepository.aggregateByProductBarcodes(any(), any(), any(), any()) }
	}

	@Test
	@DisplayName("거래처 없음 → BusinessException")
	fun accountNotFound() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(99), true) } returns emptyList()

		assertThatThrownBy { service.getElectronicSales(99, "2026-06-01", "2026-06-09", null) }
			.isInstanceOf(BusinessException::class.java)
	}

	@Test
	@DisplayName("externalKey 없으면 POS 조회 없이 빈 items·합계 0")
	fun noExternalKey() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = null))

		val res = service.getElectronicSales(1, "2026-06-01", "2026-06-09", listOf("8801234500011"))

		assertThat(res.items).isEmpty()
		assertThat(res.totalAmount).isEqualTo(0L)
		verify(exactly = 0) { liveTotSalesDailyRepository.aggregateByProductBarcodes(any(), any(), any(), any()) }
		verify(exactly = 0) { liveTotSalesDailyRepository.aggregateByCustomer(any(), any(), any()) }
	}

	@Test
	@DisplayName("POS DB 조회 실패 시 빈 items·합계 0 으로 graceful fallback")
	fun posDownGracefulFallback() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account())
		every {
			liveTotSalesDailyRepository.aggregateByProductBarcodes(any(), any(), any(), any())
		} throws RuntimeException("POS down")

		val res = service.getElectronicSales(1, "2026-06-01", "2026-06-09", listOf("8801234500011"))

		assertThat(res.items).isEmpty()
		assertThat(res.totalAmount).isEqualTo(0L)
	}
}
