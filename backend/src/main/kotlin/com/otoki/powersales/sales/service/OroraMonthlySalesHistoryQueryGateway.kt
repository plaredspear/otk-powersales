package com.otoki.powersales.sales.service

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.orora.repository.OroraMonthlySalesHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [OroraMonthlySalesHistoryRepository] 호출의 graceful fallback wrapper.
 *
 * ## 책임
 * - ORORA DataSource 호출의 예외를 흡수해 service 단에 빈 결과를 반환
 * - VPN 장애 / local 환경 (ORORA 미도달) / Hikari connection-timeout 등에서 dashboard 응답이
 *   500 으로 깨지지 않도록 절연
 *
 * ## 동작
 * - `RuntimeException` 전체 catch — `CannotCreateTransactionException`,
 *   `JDBCConnectionException`, `DataAccessException` 등 Spring/Hibernate JDBC 예외 계열 포섭
 * - 실패 시 WARN 레벨 로그 기록 (connection string / credential 노출 금지) + `emptyList()` 반환
 * - 빈 입력 (`sapAccountCodes.isEmpty()`) 시 repository 호출 자체 skip — 불필요 SQL trip 회피
 *
 * ## 환경별 동작
 * - **local/test**: ORORA 미도달 → 본 wrapper 가 WARN 로그 + emptyList 반환. dashboard 응답은
 *   정상 200 + 마감실적 0 (RDS target 만 표시)
 * - **dev/prod (VPN 정상)**: repository 정상 호출, ORORA row 반환
 * - **dev/prod (VPN 장애)**: 본 wrapper 가 WARN 로그 + emptyList 반환. dashboard 응답은 정상
 *   200 + 마감실적 0
 *
 * ## 미래 보강 여지 (본 PR 범위 외)
 * - 응답 DTO 에 `dataSourceWarning: String?` 필드 추가 → 사용자가 ORORA 장애와 진짜 0 매출을
 *   구분 가능하도록. 본 wrapper 가 실패 여부를 service 단에 전파하는 시그니처 (e.g., sealed
 *   result type) 가 필요해질 수 있음.
 */
@Component
class OroraMonthlySalesHistoryQueryGateway(
	private val repository: OroraMonthlySalesHistoryRepository,
) {
	private val logger = LoggerFactory.getLogger(javaClass)

	/**
	 * 단월 조회 — 거래처 코드 collection 이 비어 있으면 빈 결과 즉시 반환.
	 */
	fun findBySalesDate(
		salesDate: String,
		sapAccountCodes: Collection<String>,
	): List<OroraMonthlySalesHistory> {
		if (sapAccountCodes.isEmpty()) return emptyList()
		return runCatching {
			repository.findBySalesDateAndSapAccountCodeIn(salesDate, sapAccountCodes)
		}.onFailure { ex ->
			logger.warn(
				"ORORA monthly fetch failed (salesDate={}, accountCount={}): {}",
				salesDate, sapAccountCodes.size, ex.message,
			)
		}.getOrDefault(emptyList())
	}

	/**
	 * 다월 일괄 조회 — 거래처 코드 또는 매출월 collection 이 비어 있으면 빈 결과 즉시 반환.
	 */
	fun findBySalesDates(
		salesDates: Collection<String>,
		sapAccountCodes: Collection<String>,
	): List<OroraMonthlySalesHistory> {
		if (sapAccountCodes.isEmpty() || salesDates.isEmpty()) return emptyList()
		return runCatching {
			repository.findBySalesDateInAndSapAccountCodeIn(salesDates, sapAccountCodes)
		}.onFailure { ex ->
			logger.warn(
				"ORORA monthly fetch failed (salesDates={}, accountCount={}): {}",
				salesDates, sapAccountCodes.size, ex.message,
			)
		}.getOrDefault(emptyList())
	}
}
