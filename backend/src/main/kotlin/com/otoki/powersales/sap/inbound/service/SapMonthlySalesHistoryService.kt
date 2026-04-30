package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 월 매출 이력 인바운드 UPSERT 서비스. (Spec #560)
 *
 * - UPSERT 키: [MonthlySalesHistory.externalkeyC] = SAPAccountCode + SalesYearMonth (YYYYMM)
 * - SalesYearMonth substring(0,4) → salesYear, substring(4,6) → salesMonth
 * - 청크 단위 분할 + [ChunkedUpsertHelper] REQUIRES_NEW 트랜잭션
 */
@Service
class SapMonthlySalesHistoryService(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val chunkedUpsertHelper: ChunkedUpsertHelper,
    private val auditService: SapInboundAuditService,
    @Value("\${sap.inbound.sales.chunk-size:1000}") private val chunkSize: Int,
    @Value("\${sap.inbound.sales.max-rows:50000}") private val maxRows: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun upsert(items: List<MonthlySalesHistoryRequestItem>): SalesHistoryDetail {
        if (items.size > maxRows) {
            throw SapPayloadTooLargeException(maxRows, items.size)
        }

        val chunks = items.chunked(chunkSize)
        val chunkResults = mutableListOf<ChunkResult>()
        val allFailures = mutableListOf<FailureItem>()
        var totalSuccess = 0

        chunks.forEachIndexed { idx, chunk ->
            try {
                val result = chunkedUpsertHelper.processChunk(chunk) { rows -> processChunkRows(rows) }
                allFailures += result.failures
                totalSuccess += result.successCount
                val status = when {
                    result.failures.isEmpty() -> ChunkResult.STATUS_SUCCESS
                    result.successCount == 0 -> ChunkResult.STATUS_FAILED
                    else -> ChunkResult.STATUS_PARTIAL
                }
                val reason = if (result.failures.isNotEmpty()) "${result.failures.size} rows failed validation" else null
                chunkResults += ChunkResult(idx, status, chunk.size, reason)
            } catch (ex: Exception) {
                log.error("Monthly sales chunk {} commit failed: {}", idx, ex.message, ex)
                val reason = "chunk commit failed: ${ex.message?.take(150)}"
                chunkResults += ChunkResult(idx, ChunkResult.STATUS_FAILED, chunk.size, reason)
                chunk.forEach { item ->
                    allFailures += FailureItem(externalKey(item), reason)
                }
            }
        }

        recordAccepted(items.size, totalSuccess, allFailures.size, chunks.size)
        return SalesHistoryDetail(
            successCount = totalSuccess,
            failureCount = allFailures.size,
            failures = allFailures,
            chunks = chunkResults
        )
    }

    private fun processChunkRows(rows: List<MonthlySalesHistoryRequestItem>): ChunkProcessResult {
        val externalKeys = rows.mapNotNull { externalKey(it) }
        val cache: MutableMap<String, MonthlySalesHistory> = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            monthlySalesHistoryRepository.findByExternalkeyCIn(externalKeys.distinct())
                .mapNotNull { e -> e.externalkeyC?.let { it to e } }
                .toMap()
                .toMutableMap()
        }

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<MonthlySalesHistory>()

        rows.forEach { item ->
            val sapAccountCode = item.sapAccountCode?.takeIf { it.isNotBlank() }
            val salesYearMonth = item.salesYearMonth?.takeIf { it.isNotBlank() }
            if (sapAccountCode == null) {
                failures += FailureItem(null, "SAPAccountCode 필수")
                return@forEach
            }
            if (salesYearMonth == null) {
                failures += FailureItem(sapAccountCode, "SalesYearMonth 필수")
                return@forEach
            }
            if (salesYearMonth.length != 6 || !salesYearMonth.all { it.isDigit() }) {
                failures += FailureItem(sapAccountCode + salesYearMonth, "SalesYearMonth 형식 오류: $salesYearMonth")
                return@forEach
            }
            val month = salesYearMonth.substring(4, 6).toInt()
            if (month < 1 || month > 12) {
                failures += FailureItem(sapAccountCode + salesYearMonth, "SalesYearMonth 월 범위 오류: $salesYearMonth")
                return@forEach
            }

            val parsed = try {
                arrayOf(
                    parseAmount(item.abcClosingAmount1),
                    parseAmount(item.abcClosingAmount2),
                    parseAmount(item.abcClosingAmount3),
                    parseAmount(item.totalLedgerAmount),
                    parseAmount(item.shipClosingAmount),
                    parseAmount(item.rlsales)
                )
            } catch (ex: NumberFormatException) {
                failures += FailureItem(sapAccountCode + salesYearMonth, "금액 변환 실패: ${ex.message}")
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

        return ChunkProcessResult(toSave.size, failures)
    }

    private fun applyFields(entity: MonthlySalesHistory, salesYear: String, salesMonth: String, amounts: Array<Double>) {
        entity.salesYear = salesYear
        entity.salesMonth = salesMonth
        entity.abcClosingAmount1 = amounts[0]
        entity.abcClosingAmount2 = amounts[1]
        entity.abcClosingAmount3 = amounts[2]
        // TotalLedgerAmount: 신규 엔티티에 별도 컬럼 없음 (D5 — 레거시 호환만 수신, 무시)
        // 필요 시 향후 컬럼 추가
        entity.shipClosingAmount = amounts[4]
        entity.rlsalesC = amounts[5]
    }

    private fun externalKey(item: MonthlySalesHistoryRequestItem): String? {
        val ac = item.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val ym = item.salesYearMonth?.takeIf { it.isNotBlank() } ?: return null
        return ac + ym
    }

    private fun recordAccepted(received: Int, success: Int, failure: Int, chunks: Int) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = "success=$success failure=$failure chunks=$chunks"
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
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
