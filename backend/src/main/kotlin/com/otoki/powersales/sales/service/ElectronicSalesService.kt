package com.otoki.powersales.sales.service

import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sales.dto.response.ElectronicSalesResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 전산매출(ABC) 조회 service.
 *
 * ## 레거시 매핑
 * - 진입점: 레거시 `GET/POST /sales/abcMain` → `promotion/month/abcmain.jsp` (전산 매출 조회)
 * - flow-legacy: `PromotionController.abcAmount` → `PosService.SelectAbcData` →
 *   `PosMapper.xml#SelectAbcData` (POS DB `public.live_tot_sales_dh`)
 * - origin: heroku→mobile gap inventory §6 P4 (전산매출), POS DataSource 신설
 *
 * ## 레거시 동작 요약
 * - 입력: 거래처(`CUST_CD`) 1곳 + 기간(`YMD_ID BETWEEN`) → 제품별 `SUM(SALES_RAMT/RQTY)` 집계
 * - 부수효과: 없음 (순수 조회)
 *
 * ## 데이터 source (신규)
 * POS DB `public.live_tot_sales_dh` ([com.otoki.pos.entity.LiveTotSalesDaily]) 직 SELECT.
 * 거래처 코드는 [com.otoki.powersales.domain.foundation.account.entity.Account.externalKey] 에 레거시 패딩
 * `"000" + accountCode` ([CUST_CD_PREFIX]) 를 적용해 `CUST_CD` 키로 사용.
 *
 * ## 기간 / 매출 조회 제품 (레거시 정합)
 * - 기간: 레거시 daterangepicker 와 동일하게 시작일~종료일(`YYYY-MM-DD`) 을 그대로 받아
 *   `YMD_ID BETWEEN` 으로 사용한다.
 * - 매출 조회 제품(`barcodes`): 레거시 `productCd` IN 분기와 동일.
 *   - 제품 선택 시(`abcAmount`): `UPC_CD IN` 으로 제품별 집계, 합계금액 = 선택 제품 금액 합.
 *   - 미선택 시(`abcSumAmount`): 제품 명세 없이 거래처·기간 전체 `SUM(SALES_RAMT)` 합계만.
 *
 * ## graceful fallback
 * POS DB 도달 불가(미설정/VPN 장애) 시 빈 결과로 응답 — ORORA gateway 와 동일 정책으로 메인
 * 응답성을 보호한다.
 */
@Service
@Transactional(readOnly = true)
class ElectronicSalesService(
	private val accountRepository: AccountRepository,
	private val liveTotSalesDailyRepository: LiveTotSalesDailyRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun getElectronicSales(
		customerId: Long,
		startDate: String,
		endDate: String,
		barcodes: List<String>?,
	): ElectronicSalesResponse {
		val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
			?: throw BusinessException(
				errorCode = "ACCOUNT_NOT_FOUND",
				message = "거래처를 찾을 수 없습니다: $customerId",
				httpStatus = HttpStatus.NOT_FOUND,
			)

		val sapCode = account.externalKey
		val custCd = if (sapCode.isNullOrBlank()) null else "$CUST_CD_PREFIX$sapCode"
		val selectedBarcodes = barcodes?.filter { it.isNotBlank() } ?: emptyList()

		val (items, totalAmount) = when {
			custCd == null -> emptyList<ElectronicSalesResponse.ProductSales>() to 0L
			selectedBarcodes.isEmpty() -> emptyList<ElectronicSalesResponse.ProductSales>() to
				sumAmount(custCd, startDate, endDate)
			else -> aggregateProducts(custCd, startDate, endDate, selectedBarcodes)
		}

		return ElectronicSalesResponse(
			customerId = account.id,
			customerName = account.name ?: "",
			sapAccountCode = sapCode ?: "",
			startDate = startDate,
			endDate = endDate,
			totalAmount = totalAmount,
			items = items,
		)
	}

	/** 매출 조회 제품 선택 시: `UPC_CD IN` 제품별 집계 + 선택 제품 금액 합 (레거시 `abcAmount`). */
	private fun aggregateProducts(
		custCd: String,
		startDate: String,
		endDate: String,
		barcodes: List<String>,
	): Pair<List<ElectronicSalesResponse.ProductSales>, Long> = runCatching {
		val items = liveTotSalesDailyRepository
			.aggregateByProductBarcodes(custCd, startDate, endDate, barcodes)
			.map { row ->
				ElectronicSalesResponse.ProductSales(
					productCode = row.getItemCd(),
					productName = row.getItemNm() ?: "",
					barcode = row.getBarcode() ?: "",
					amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
					quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
				)
			}
		items to items.sumOf { it.amount }
	}.getOrElse { e ->
		log.warn("POS 전산매출 제품 조회 실패 (custCd={}, {}~{}): {}", custCd, startDate, endDate, e.message)
		emptyList<ElectronicSalesResponse.ProductSales>() to 0L
	}

	/** 매출 조회 제품 미선택 시: 거래처·기간 전체 합계금액 (레거시 `abcSumAmount`). */
	private fun sumAmount(custCd: String, startDate: String, endDate: String): Long = runCatching {
		liveTotSalesDailyRepository.aggregateByCustomer(listOf(custCd), startDate, endDate)
			.firstOrNull()
			?.getSalesAmt()
			?.toLong() ?: 0L
	}.getOrElse { e ->
		log.warn("POS 전산매출 합계 조회 실패 (custCd={}, {}~{}): {}", custCd, startDate, endDate, e.message)
		0L
	}

	companion object {
		/** 레거시 `abcmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
		private const val CUST_CD_PREFIX = "000"
	}
}
