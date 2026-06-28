package com.otoki.powersales.domain.sales.service

import com.otoki.pos.repository.LivePosSalesDailyRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * POS매출 조회 service.
 *
 * ## 레거시 매핑
 * - 진입점: 레거시 `GET /sales/posMain` + `POST /sales/posAmount` → `promotion/month/posmain.jsp` (POS매출 조회)
 * - flow-legacy: `PromotionController.posAmount` → `PosService.selectPosData` →
 *   `PosMapper.xml#selectPosData` (POS DB `public.live_pos_sales_dh`)
 * - origin: heroku POS매출 → web admin 이관 (즉시 구현 요청)
 *
 * ## 레거시 동작 요약
 * - 입력: 거래처(`CUST_CD`) 1곳 + 기간(`DATE BETWEEN`) → 제품별 `SUM(SALES_AMT/QTY)` 집계
 * - 부수효과: 없음 (순수 조회)
 *
 * ## 데이터 source (신규)
 * POS DB `public.live_pos_sales_dh` ([com.otoki.pos.entity.LivePosSalesDaily]) 직 SELECT.
 * 거래처 코드는 [com.otoki.powersales.domain.foundation.account.entity.Account.externalKey] 에 레거시 패딩
 * `"000" + accountCode` ([CUST_CD_PREFIX]) 를 적용해 `CUST_CD` 키로 사용.
 *
 * ## 기간 산출
 * 레거시 daterangepicker 는 "당월 1일~오늘" 기본(최대 31일)이나, 신규는 연월(`YYYYMM`) 단위 조회로
 * **해당 월 1일~말일** 범위를 사용 (월 POS매출 누적). 전산매출 service 와 동일 정책.
 *
 * ## graceful fallback
 * POS DB 도달 불가(미설정/VPN 장애) 시 빈 결과로 응답 — 전산매출 service 와 동일 정책으로 메인
 * 응답성을 보호한다.
 */
@Service
@Transactional(readOnly = true)
class PosSalesService(
	private val accountRepository: AccountRepository,
	private val livePosSalesDailyRepository: LivePosSalesDailyRepository,
	private val posSalesExcelExporter: PosSalesExcelExporter,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	/**
	 * 거래처 1곳 + 연월의 제품별 POS매출 명세를 xlsx 로 export — 조회([getPosSales])와 동일 데이터.
	 *
	 * 조회와 동일하게 거래처 미존재 시 404, POS DB 장애 시 빈 명세로 fallback (헤더만 있는 엑셀).
	 */
	fun exportPosSales(customerId: Long, yearMonth: String): ExcelResult {
		val response = getPosSales(customerId, yearMonth)
		return posSalesExcelExporter.export(response.customerName, response.yearMonth, response.items)
	}

	/**
	 * 거래처 1곳 + 연월의 제품별 POS매출 조회.
	 *
	 * 거래처 미존재 시 404. 거래처 SAP 코드(`externalKey`) 부재 또는 POS DB 장애 시 빈 명세로 fallback.
	 */
	fun getPosSales(customerId: Long, yearMonth: String): PosSalesResponse {
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
				livePosSalesDailyRepository.aggregateByProduct(custCd, startDate, endDate).map { row ->
					PosSalesResponse.ProductSales(
						productCode = row.getItemCd(),
						productName = row.getItemNm() ?: "",
						barcode = row.getBarcode(),
						amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
						quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
					)
				}
			}.getOrElse { e ->
				log.warn(
					"POS매출 조회 실패 (custCd={}, {}~{}): {}",
					custCd, startDate, endDate, e.message,
				)
				emptyList()
			}
		}

		return PosSalesResponse(
            customerId = account.id,
            customerName = account.name ?: "",
            sapAccountCode = sapCode ?: "",
            yearMonth = yearMonth,
            items = items,
        )
	}

	/**
	 * 거래처 1곳 + 기간(시작/종료일) + 선택 바코드 목록의 제품별 POS매출 명세를 xlsx 로 export —
	 * 조회([getPosSalesByRange])와 동일 데이터. web admin POS매출 화면의 기간 조회 엑셀 다운로드용.
	 *
	 * [barcodes] 가 비면 거래처 전체 제품, 1건 이상이면 해당 바코드 제품만 집계 (조회와 동일 규칙).
	 * 조회와 동일하게 거래처 미존재 시 404, POS DB 장애 시 빈 명세로 fallback (헤더만 있는 엑셀).
	 */
	fun exportPosSalesByRange(
		customerId: Long,
		startDate: String,
		endDate: String,
		barcodes: List<String>?,
	): ExcelResult {
		val response = getPosSalesByRange(customerId, startDate, endDate, barcodes)
		return posSalesExcelExporter.exportByRange(
			response.customerName, response.startDate, response.endDate, response.items,
		)
	}

	/**
	 * 거래처 1곳 + 기간(시작/종료일) + 선택 바코드 목록의 제품별 POS매출 조회 (레거시 daterangepicker 정합).
	 *
	 * [barcodes] 가 비면 거래처 전체 제품을 집계(레거시 `posSumAmount` 합계 모드), 1건 이상이면 해당
	 * 바코드 제품만 집계(레거시 `posAmount` 명세 모드)한다. 합계금액/수량은 명세를 서버에서 합산한다.
	 *
	 * 거래처 미존재 시 404. 거래처 SAP 코드(`externalKey`) 부재 또는 POS DB 장애 시 빈 명세로 fallback.
	 */
	fun getPosSalesByRange(
		customerId: Long,
		startDate: String,
		endDate: String,
		barcodes: List<String>?,
	): PosSalesRangeResponse {
		val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
			?: throw BusinessException(
				errorCode = "ACCOUNT_NOT_FOUND",
				message = "거래처를 찾을 수 없습니다: $customerId",
				httpStatus = HttpStatus.NOT_FOUND,
			)

		val sapCode = account.externalKey
		val custCd = if (sapCode.isNullOrBlank()) null else "$CUST_CD_PREFIX$sapCode"

		// 공백 제거 후 비어있지 않은 바코드만 사용 (빈 IN () SQL 오류 방지)
		val filterBarcodes = barcodes?.mapNotNull { it.trim().ifBlank { null } }?.distinct() ?: emptyList()

		val items = if (custCd == null) {
			emptyList()
		} else {
			runCatching {
				val rows = if (filterBarcodes.isEmpty()) {
					livePosSalesDailyRepository.aggregateByProduct(custCd, startDate, endDate)
				} else {
					livePosSalesDailyRepository.aggregateByProductAndBarcodes(custCd, startDate, endDate, filterBarcodes)
				}
				rows.map { row ->
					PosSalesResponse.ProductSales(
						productCode = row.getItemCd(),
						productName = row.getItemNm() ?: "",
						barcode = row.getBarcode(),
						amount = (row.getSalesAmt() ?: BigDecimal.ZERO).toLong(),
						quantity = (row.getSalesQty() ?: BigDecimal.ZERO).toLong(),
					)
				}
			}.getOrElse { e ->
				log.warn(
					"POS매출(기간) 조회 실패 (custCd={}, {}~{}, barcodes={}): {}",
					custCd, startDate, endDate, filterBarcodes.size, e.message,
				)
				emptyList()
			}
		}

		return PosSalesRangeResponse(
            customerId = account.id,
            customerName = account.name ?: "",
            sapAccountCode = sapCode ?: "",
            startDate = startDate,
            endDate = endDate,
            totalAmount = items.sumOf { it.amount },
            totalQuantity = items.sumOf { it.quantity },
            items = items,
        )
	}

	companion object {
		private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

		/** 레거시 `posmain.jsp` 의 `CUST_CD = "000" + accountCode` 패딩 정합. */
		private const val CUST_CD_PREFIX = "000"
	}
}
