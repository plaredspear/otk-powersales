package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertFailedRow
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertResult
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * 월 매출 이력 UPSERT 도메인 서비스 (단일 청크 단위).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.sap.inbound.service.SapMonthlySalesHistoryService]
 * - origin spec: #560 (SAP 매출 이력 인바운드) + #575 (TotalLedgerAmount 보존) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<MonthlySalesHistoryUpsertCommand>` — 어댑터가 분할한 단일 청크.
 * 2. 캐시 빌드: [MonthlySalesHistoryRepository.findByExternalkeyCIn] (UPSERT 키 = sapAccountCode + salesYearMonth).
 * 3. 행 단위 검증/변환/적용:
 *    - 필수값 (`sapAccountCode`/`salesYearMonth`) 누락 → failures.
 *    - SalesYearMonth 형식 (6자 숫자) 또는 월 범위 (1..12) 위반 → failures.
 *    - 금액 변환 (ABCClosingAmount1~3 / TotalLedgerAmount / ShipClosingAmount / rlsales, blank/`"0"` → 0.0) 실패 → failures.
 *    - 정상 행: 신규 [MonthlySalesHistory] 생성 또는 기존 entity 의 mutable 금액 갱신.
 *    - SalesYearMonth substring(0,4) → salesYear, substring(4,6) → salesMonth.
 * 4. 외부 호출: [MonthlySalesHistoryRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * 트랜잭션 경계는 어댑터의 [com.otoki.powersales.sap.inbound.service.ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 * 도메인 서비스 자체에는 `@Transactional` 을 부착하지 않는다.
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class MonthlySalesHistoryUpsertService(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository
) {

    fun upsert(commands: List<MonthlySalesHistoryUpsertCommand>): MonthlySalesHistoryUpsertResult {
        val externalKeys = commands.mapNotNull { externalKey(it) }
        val cache: MutableMap<String, MonthlySalesHistory> = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            monthlySalesHistoryRepository.findByExternalkeyCIn(externalKeys.distinct())
                .mapNotNull { e -> e.externalkeyC?.let { it to e } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<MonthlySalesHistoryUpsertFailedRow>()
        val toSave = mutableListOf<MonthlySalesHistory>()

        commands.forEach { command ->
            val sapAccountCode = command.sapAccountCode?.takeIf { it.isNotBlank() }
            val salesYearMonth = command.salesYearMonth?.takeIf { it.isNotBlank() }
            if (sapAccountCode == null) {
                failures += MonthlySalesHistoryUpsertFailedRow(null, "SAPAccountCode 필수")
                return@forEach
            }
            if (salesYearMonth == null) {
                failures += MonthlySalesHistoryUpsertFailedRow(sapAccountCode, "SalesYearMonth 필수")
                return@forEach
            }
            if (salesYearMonth.length != 6 || !salesYearMonth.all { it.isDigit() }) {
                failures += MonthlySalesHistoryUpsertFailedRow(sapAccountCode + salesYearMonth, "SalesYearMonth 형식 오류: $salesYearMonth")
                return@forEach
            }
            val month = salesYearMonth.substring(4, 6).toInt()
            if (month < 1 || month > 12) {
                failures += MonthlySalesHistoryUpsertFailedRow(sapAccountCode + salesYearMonth, "SalesYearMonth 월 범위 오류: $salesYearMonth")
                return@forEach
            }

            val parsed = try {
                arrayOf(
                    parseAmount(command.abcClosingAmount1),
                    parseAmount(command.abcClosingAmount2),
                    parseAmount(command.abcClosingAmount3),
                    parseAmount(command.totalLedgerAmount),
                    parseAmount(command.shipClosingAmount),
                    parseAmount(command.rlsales)
                )
            } catch (ex: NumberFormatException) {
                failures += MonthlySalesHistoryUpsertFailedRow(sapAccountCode + salesYearMonth, "금액 변환 실패: ${ex.message}")
                return@forEach
            }

            val key = sapAccountCode + salesYearMonth
            val salesYear = salesYearMonth.substring(0, 4)
            val salesMonth = salesYearMonth.substring(4, 6)

            val entity = cache[key]?.also { applyFields(it, salesYear, salesMonth, parsed) }
                ?: MonthlySalesHistory(externalkeyC = key).also {
                    applyFields(it, salesYear, salesMonth, parsed)
                    cache[key] = it
                }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            monthlySalesHistoryRepository.saveAll(toSave)
        }

        return MonthlySalesHistoryUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun applyFields(entity: MonthlySalesHistory, salesYear: String, salesMonth: String, amounts: Array<Double>) {
        entity.salesYear = salesYear
        entity.salesMonth = salesMonth
        entity.abcClosingAmount1 = amounts[0]
        entity.abcClosingAmount2 = amounts[1]
        entity.abcClosingAmount3 = amounts[2]
        entity.totalLedgerAmount = BigDecimal.valueOf(amounts[3]) // Spec #575: SAP TotalLedgerAmount 보존
        entity.shipClosingAmount = amounts[4]
        entity.rlsalesC = amounts[5]
    }

    private fun externalKey(command: MonthlySalesHistoryUpsertCommand): String? {
        val ac = command.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val ym = command.salesYearMonth?.takeIf { it.isNotBlank() } ?: return null
        return ac + ym
    }

    companion object {
        @Throws(NumberFormatException::class)
        fun parseAmount(value: String?): Double {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty() || trimmed == "0") return 0.0
            return trimmed.toDouble()
        }
    }
}
