package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertFailedRow
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertResult
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 일 매출 이력 UPSERT 도메인 서비스 (단일 청크 단위).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.sap.inbound.service.SapDailySalesHistoryService]
 * - origin spec: #560 (SAP 매출 이력 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<DailySalesHistoryUpsertCommand>` — 어댑터가 분할한 단일 청크.
 * 2. 캐시 빌드: [DailySalesHistoryRepository.findByExternalKeyIn] (UPSERT 키 = sapAccountCode + salesDate).
 * 3. 행 단위 검증/변환/적용:
 *    - 필수값 (`sapAccountCode`/`salesDate`) 누락 → failures.
 *    - SalesDate 형식 (`yyyyMMdd`) 위반 → failures.
 *    - 금액 변환 (ERPSalesAmount1~3 / ERPDistributionAmount1~3 / LedgerAmount, blank/`"0"` → 0.0) 실패 → failures.
 *    - 정상 행: 신규 [DailySalesHistory] 생성 또는 기존 entity 의 mutable 금액 갱신.
 * 4. 외부 호출: [DailySalesHistoryRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * 트랜잭션 경계는 어댑터의 [com.otoki.powersales.sap.inbound.service.ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 * 도메인 서비스 자체에는 `@Transactional` 을 부착하지 않는다 — helper 의 트랜잭션 안에서 호출된다.
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class DailySalesHistoryUpsertService(
    private val dailySalesHistoryRepository: DailySalesHistoryRepository
) {

    fun upsert(commands: List<DailySalesHistoryUpsertCommand>): DailySalesHistoryUpsertResult {
        val externalKeys = commands.mapNotNull { externalKey(it) }
        val cache: MutableMap<String, DailySalesHistory> = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            dailySalesHistoryRepository.findByExternalKeyIn(externalKeys.distinct())
                .associateBy { it.externalKey }
                .toMutableMap()
        }

        val failures = mutableListOf<DailySalesHistoryUpsertFailedRow>()
        val toSave = mutableListOf<DailySalesHistory>()

        commands.forEach { command ->
            val sapAccountCode = command.sapAccountCode?.takeIf { it.isNotBlank() }
            val salesDate = command.salesDate?.takeIf { it.isNotBlank() }
            if (sapAccountCode == null) {
                failures += DailySalesHistoryUpsertFailedRow(null, "SAPAccountCode 필수")
                return@forEach
            }
            if (salesDate == null) {
                failures += DailySalesHistoryUpsertFailedRow(sapAccountCode, "SalesDate 필수")
                return@forEach
            }
            if (!isValidYyyymmdd(salesDate)) {
                failures += DailySalesHistoryUpsertFailedRow(sapAccountCode + salesDate, "SalesDate 형식 오류: $salesDate")
                return@forEach
            }

            val parsed = try {
                arrayOf(
                    parseAmount(command.erpSalesAmount1),
                    parseAmount(command.erpSalesAmount2),
                    parseAmount(command.erpSalesAmount3),
                    parseAmount(command.erpDistributionAmount1),
                    parseAmount(command.erpDistributionAmount2),
                    parseAmount(command.erpDistributionAmount3),
                    parseAmount(command.ledgerAmount)
                )
            } catch (ex: NumberFormatException) {
                failures += DailySalesHistoryUpsertFailedRow(sapAccountCode + salesDate, "금액 변환 실패: ${ex.message}")
                return@forEach
            }

            val key = sapAccountCode + salesDate
            val entity = cache[key]?.also { applyAmounts(it, parsed) }
                ?: DailySalesHistory(
                    sapAccountCode = sapAccountCode,
                    salesDate = salesDate,
                    externalKey = key
                ).also {
                    applyAmounts(it, parsed)
                    cache[key] = it
                }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            dailySalesHistoryRepository.saveAll(toSave)
        }

        return DailySalesHistoryUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun applyAmounts(entity: DailySalesHistory, amounts: Array<Double>) {
        entity.erpSalesAmount1 = amounts[0]
        entity.erpSalesAmount2 = amounts[1]
        entity.erpSalesAmount3 = amounts[2]
        entity.erpDistributionAmount1 = amounts[3]
        entity.erpDistributionAmount2 = amounts[4]
        entity.erpDistributionAmount3 = amounts[5]
        entity.ledgerAmount = amounts[6]
    }

    private fun externalKey(command: DailySalesHistoryUpsertCommand): String? {
        val ac = command.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val sd = command.salesDate?.takeIf { it.isNotBlank() } ?: return null
        return ac + sd
    }

    private fun isValidYyyymmdd(value: String): Boolean = try {
        LocalDate.parse(value, DATE_FORMAT)
        true
    } catch (_: DateTimeParseException) {
        false
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        @Throws(NumberFormatException::class)
        fun parseAmount(value: String?): Double {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty() || trimmed == "0") return 0.0
            return trimmed.toDouble()
        }
    }
}
