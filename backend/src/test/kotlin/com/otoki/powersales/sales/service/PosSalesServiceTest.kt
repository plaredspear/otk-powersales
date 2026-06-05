package com.otoki.powersales.sales.service

import com.otoki.pos.repository.LivePosSalesDailyRepository
import com.otoki.pos.repository.PosSalesRow
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
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

@DisplayName("PosSalesService 테스트")
class PosSalesServiceTest {

	private val accountRepository: AccountRepository = mockk()
	private val livePosSalesDailyRepository: LivePosSalesDailyRepository = mockk()

	private val service = PosSalesService(accountRepository, livePosSalesDailyRepository)

	private fun account(id: Long = 1L, name: String = "사과마을", externalKey: String? = "12345") =
		Account(id = id, name = name, externalKey = externalKey)

	private fun row(itemCd: String, itemNm: String?, barcode: String?, amt: Long, qty: Long): PosSalesRow {
		val r = mockk<PosSalesRow>()
		every { r.getItemCd() } returns itemCd
		every { r.getItemNm() } returns itemNm
		every { r.getBarcode() } returns barcode
		every { r.getSalesAmt() } returns BigDecimal.valueOf(amt)
		every { r.getSalesQty() } returns BigDecimal.valueOf(qty)
		return r
	}

	@Test
	@DisplayName("externalKey 에 '000' 패딩한 CUST_CD + 월 1일~말일 범위로 조회하고 제품별 매핑")
	fun querySourceAndMapping() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = "12345"))
		val custSlot = slot<String>()
		val startSlot = slot<String>()
		val endSlot = slot<String>()
		every {
			livePosSalesDailyRepository.aggregateByProduct(capture(custSlot), capture(startSlot), capture(endSlot))
		} returns listOf(
			row("01101123", "갈릭 아이올리소스 240g", "8801045123456", 3500, 10),
			row("01101222", "오뚜기 3분 카레 100g", "8801045222333", 1500, 5),
		)

		val res = service.getPosSales(1, "202602")

		// 레거시 CUST_CD = "000" + accountCode
		assertThat(custSlot.captured).isEqualTo("00012345")
		// 월 1일 ~ 말일 (2026-02 은 28일)
		assertThat(startSlot.captured).isEqualTo("2026-02-01")
		assertThat(endSlot.captured).isEqualTo("2026-02-28")

		assertThat(res.customerId).isEqualTo(1)
		assertThat(res.sapAccountCode).isEqualTo("12345")
		assertThat(res.yearMonth).isEqualTo("202602")
		assertThat(res.items).hasSize(2)
		assertThat(res.items[0].productCode).isEqualTo("01101123")
		assertThat(res.items[0].productName).isEqualTo("갈릭 아이올리소스 240g")
		assertThat(res.items[0].barcode).isEqualTo("8801045123456")
		assertThat(res.items[0].amount).isEqualTo(3500L)
		assertThat(res.items[0].quantity).isEqualTo(10L)
	}

	@Test
	@DisplayName("거래처 없음 → BusinessException")
	fun accountNotFound() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(99), true) } returns emptyList()

		assertThatThrownBy { service.getPosSales(99, "202602") }
			.isInstanceOf(BusinessException::class.java)
	}

	@Test
	@DisplayName("externalKey 없으면 POS 조회 없이 빈 items")
	fun noExternalKey() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = null))

		val res = service.getPosSales(1, "202602")

		assertThat(res.items).isEmpty()
		verify(exactly = 0) { livePosSalesDailyRepository.aggregateByProduct(any(), any(), any()) }
	}

	@Test
	@DisplayName("POS DB 조회 실패 시 빈 items 로 graceful fallback")
	fun posDownGracefulFallback() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account())
		every { livePosSalesDailyRepository.aggregateByProduct(any(), any(), any()) } throws RuntimeException("POS down")

		val res = service.getPosSales(1, "202602")

		assertThat(res.items).isEmpty()
	}
}
