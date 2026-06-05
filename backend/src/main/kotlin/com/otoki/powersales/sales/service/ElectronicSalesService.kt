package com.otoki.powersales.sales.service

import com.otoki.pos.repository.LiveTotSalesDailyRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.sales.dto.response.ElectronicSalesResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
 * 거래처 코드는 [com.otoki.powersales.account.entity.Account.externalKey] 에 레거시 패딩
 * `"000" + accountCode` ([CUST_CD_PREFIX]) 를 적용해 `CUST_CD` 키로 사용.
 *
 * ## 기간 산출
 * 레거시 daterangepicker 는 "당월 1일~오늘" 기본이나, 신규는 연월(`YYYYMM`) 단위 조회로
 * **해당 월 1일~말일** 범위를 사용 (월 전산매출 누적).
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

	fun getElectronicSales(customerId: Long, yearMonth: String): ElectronicSalesResponse {
		val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
			?: throw BusinessException(
				errorCode = "ACCOUNT_NOT_FOUND",
				message = "거래처를 찾을 수 없습니다: $customerId",
				httpStatus = HttpStatus.NOT_FOUND,
			)

		val year = yearMonth.substring(0, 4).toInt()
		val month = yearMonth.substring(4, 6).toInt()
		val firstDay = LocalDate.of(year, month, 1)
		val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
		val startDate = firstDay.format(DATE_FORMATTER)
		val endDate = lastDay.format(DATE_FORMATTER)

		val sapCode = account.externalKey
		val custCd = if (sapCode.isNullOrBlank()) null else "$CUST_CD_PREFIX$sapCode"

		val items = if (custCd == null) {
			emptyList()
		} else {
			runCatching {
				liveTotSalesDailyRepository.aggregateByProduct(custCd, startDate, endDate).map { row ->
					ElectronicSalesResponse.ProductSales(
						productCode = row.getItemCd(),
						productName = row.getItemNm() ?: "",
						amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
						quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
					)
				}
			}.getOrElse { e ->
				log.warn(
					"POS 전산매출 조회 실패 (custCd={}, {}~{}): {}",
					custCd, startDate, endDate, e.message,
				)
				emptyList()
			}
		}

		return ElectronicSalesResponse(
			customerId = account.id,
			customerName = account.name ?: "",
			sapAccountCode = sapCode ?: "",
			yearMonth = yearMonth,
			items = items,
		)
	}

	companion object {
		private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

		/** 레거시 `abcmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
		private const val CUST_CD_PREFIX = "000"
	}
}
