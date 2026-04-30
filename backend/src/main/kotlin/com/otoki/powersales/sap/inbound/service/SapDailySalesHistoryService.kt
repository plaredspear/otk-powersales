package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.entity.DailySalesHistory
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.DailySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sap.repository.DailySalesHistoryRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * SAP 일 매출 이력 인바운드 UPSERT 서비스. (Spec #560)
 *
 * - UPSERT 키: [DailySalesHistory.externalKey] = SAPAccountCode + SalesDate (YYYYMMDD)
 * - 청크 단위 분할 + 청크별 [ChunkedUpsertHelper] 가 REQUIRES_NEW 트랜잭션 처리
 * - 청크 commit 실패 시 그 청크 전체 failed, 다른 청크는 처리 진행
 */
@Service
class SapDailySalesHistoryService(
    private val dailySalesHistoryRepository: DailySalesHistoryRepository,
    private val chunkedUpsertHelper: ChunkedUpsertHelper,
    private val auditService: SapInboundAuditService,
    @Value("\${sap.inbound.sales.chunk-size:1000}") private val chunkSize: Int,
    @Value("\${sap.inbound.sales.max-rows:50000}") private val maxRows: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun upsert(items: List<DailySalesHistoryRequestItem>): SalesHistoryDetail {
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
                log.error("Daily sales chunk {} commit failed: {}", idx, ex.message, ex)
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

    private fun processChunkRows(rows: List<DailySalesHistoryRequestItem>): ChunkProcessResult {
        val externalKeys = rows.mapNotNull { externalKey(it) }
        val cache: MutableMap<String, DailySalesHistory> = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            dailySalesHistoryRepository.findByExternalKeyIn(externalKeys.distinct())
                .associateBy { it.externalKey }
                .toMutableMap()
        }

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<DailySalesHistory>()

        rows.forEach { item ->
            val sapAccountCode = item.sapAccountCode?.takeIf { it.isNotBlank() }
            val salesDate = item.salesDate?.takeIf { it.isNotBlank() }
            if (sapAccountCode == null) {
                failures += FailureItem(null, "SAPAccountCode 필수")
                return@forEach
            }
            if (salesDate == null) {
                failures += FailureItem(sapAccountCode, "SalesDate 필수")
                return@forEach
            }
            if (!isValidYyyymmdd(salesDate)) {
                failures += FailureItem(sapAccountCode + salesDate, "SalesDate 형식 오류: $salesDate")
                return@forEach
            }

            val parsed = try {
                arrayOf(
                    parseAmount(item.erpSalesAmount1),
                    parseAmount(item.erpSalesAmount2),
                    parseAmount(item.erpSalesAmount3),
                    parseAmount(item.erpDistributionAmount1),
                    parseAmount(item.erpDistributionAmount2),
                    parseAmount(item.erpDistributionAmount3),
                    parseAmount(item.ledgerAmount)
                )
            } catch (ex: NumberFormatException) {
                failures += FailureItem(sapAccountCode + salesDate, "금액 변환 실패: ${ex.message}")
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

        return ChunkProcessResult(toSave.size, failures)
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

    private fun externalKey(item: DailySalesHistoryRequestItem): String? {
        val ac = item.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val sd = item.salesDate?.takeIf { it.isNotBlank() } ?: return null
        return ac + sd
    }

    private fun isValidYyyymmdd(value: String): Boolean = try {
        LocalDate.parse(value, DATE_FORMAT)
        true
    } catch (_: DateTimeParseException) {
        false
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
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        @Throws(NumberFormatException::class)
        fun parseAmount(value: String?): Double {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty() || trimmed == "0") return 0.0
            return trimmed.toDouble()
        }
    }
}
