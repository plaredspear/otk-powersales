package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.LivePosSalesDailyRepository
import com.otoki.pos.repository.PosSalesRow
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal

@DisplayName("PosSalesService 테스트")
class PosSalesServiceTest {

	private val accountRepository: AccountRepository = mockk()
	private val livePosSalesDailyRepository: LivePosSalesDailyRepository = mockk()

	private val service = PosSalesService(accountRepository, livePosSalesDailyRepository, PosSalesExcelExporter())

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

	@Test
	@DisplayName("exportPosSales - 조회와 동일 데이터를 헤더 5컬럼 + 데이터 행으로 출력 + 파일명에 거래처/연월")
	fun exportPosSales_success() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(name = "사과마을"))
		every { livePosSalesDailyRepository.aggregateByProduct(any(), any(), any()) } returns listOf(
			row("01101123", "갈릭 아이올리소스 240g", "8801045123456", 3500, 10),
		)

		val result = service.exportPosSales(1, "202602")

		assertThat(result.filename).isEqualTo("POS매출_사과마을_202602.xlsx")
		val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
		val sheet = workbook.getSheetAt(0)
		assertThat(sheet.sheetName).isEqualTo("POS매출")
		assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("제품코드")
		assertThat(sheet.getRow(0).getCell(4).stringCellValue).isEqualTo("금액(원)")
		val dataRow = sheet.getRow(1)
		assertThat(dataRow.getCell(0).stringCellValue).isEqualTo("01101123")
		assertThat(dataRow.getCell(1).stringCellValue).isEqualTo("갈릭 아이올리소스 240g")
		assertThat(dataRow.getCell(2).stringCellValue).isEqualTo("8801045123456")
		assertThat(dataRow.getCell(3).numericCellValue).isEqualTo(10.0)
		assertThat(dataRow.getCell(4).numericCellValue).isEqualTo(3500.0)
		workbook.close()
	}

	@Test
	@DisplayName("exportPosSales - POS DB 장애 시 헤더만 있는 빈 엑셀로 fallback")
	fun exportPosSales_emptyFallback() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = null))

		val result = service.exportPosSales(1, "202602")

		val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
		assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0) // 헤더 행만
		workbook.close()
	}

	// ── getPosSalesByRange (기간 + 바코드 필터) ───────────────────────────

	@Test
	@DisplayName("getPosSalesByRange - barcodes 비면 전체 제품 집계(aggregateByProduct) + 합계 서버 산출")
	fun getPosSalesByRange_noBarcodes() {
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

		val res = service.getPosSalesByRange(1, "2026-02-01", "2026-02-15", emptyList())

		assertThat(custSlot.captured).isEqualTo("00012345")
		assertThat(startSlot.captured).isEqualTo("2026-02-01")
		assertThat(endSlot.captured).isEqualTo("2026-02-15")
		// 바코드 미지정 → 바코드 필터 메서드는 호출되지 않음.
		verify(exactly = 0) {
			livePosSalesDailyRepository.aggregateByProductAndBarcodes(any(), any(), any(), any())
		}
		assertThat(res.items).hasSize(2)
		// 합계는 서버 산출분 (명세 합산).
		assertThat(res.totalAmount).isEqualTo(5000L)
		assertThat(res.totalQuantity).isEqualTo(15L)
	}

	@Test
	@DisplayName("getPosSalesByRange - barcodes 1건+ 이면 바코드 필터 집계 + 공백/중복 정제 후 전달")
	fun getPosSalesByRange_withBarcodes() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = "12345"))
		val barcodeSlot = slot<List<String>>()
		every {
			livePosSalesDailyRepository.aggregateByProductAndBarcodes(any(), any(), any(), capture(barcodeSlot))
		} returns listOf(
			row("01101123", "갈릭 아이올리소스 240g", "8801045123456", 3500, 10),
		)

		// 공백 패딩 + 빈 값 + 중복 포함 입력 → trim/blank 제외/distinct 정제되어 전달되어야 한다.
		val res = service.getPosSalesByRange(
			1, "2026-02-01", "2026-02-15",
			listOf(" 8801045123456 ", "", "8801045123456", "8801045999999"),
		)

		assertThat(barcodeSlot.captured).containsExactly("8801045123456", "8801045999999")
		verify(exactly = 0) {
			livePosSalesDailyRepository.aggregateByProduct(any(), any(), any())
		}
		assertThat(res.items).hasSize(1)
		assertThat(res.totalAmount).isEqualTo(3500L)
		assertThat(res.totalQuantity).isEqualTo(10L)
	}

	@Test
	@DisplayName("getPosSalesByRange - POS DB 장애 시 빈 명세로 fallback")
	fun getPosSalesByRange_emptyFallback() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = "12345"))
		every {
			livePosSalesDailyRepository.aggregateByProduct(any(), any(), any())
		} throws RuntimeException("POS down")

		val res = service.getPosSalesByRange(1, "2026-02-01", "2026-02-15", emptyList())

		assertThat(res.items).isEmpty()
		assertThat(res.totalAmount).isEqualTo(0L)
		assertThat(res.totalQuantity).isEqualTo(0L)
	}

	@Test
	@DisplayName("getPosSalesByRange - 거래처 SAP 코드 없으면 빈 명세 (POS 조회 미수행)")
	fun getPosSalesByRange_noSapCode() {
		every { accountRepository.findByIdInAndIsDeletedNot(listOf(1), true) } returns listOf(account(externalKey = null))

		val res = service.getPosSalesByRange(1, "2026-02-01", "2026-02-15", listOf("8801045123456"))

		assertThat(res.items).isEmpty()
		verify(exactly = 0) {
			livePosSalesDailyRepository.aggregateByProductAndBarcodes(any(), any(), any(), any())
		}
	}
}
