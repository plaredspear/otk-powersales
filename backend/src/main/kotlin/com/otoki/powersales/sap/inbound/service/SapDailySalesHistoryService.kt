package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sales.service.DailySalesHistoryUpsertService
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.DailySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 일 매출 이력 인바운드 어댑터. (Spec #560 / 어댑터-도메인 분리: #635 P1-B)
 *
 * 책임:
 * - SAP 페이로드 size 한도 검증 ([SapPayloadTooLargeException])
 * - 청크 분할 ([items.chunked]) + 청크 단위 [ChunkedUpsertHelper] 트랜잭션 격리
 * - 청크별로 페이로드 → 도메인 커맨드 [DailySalesHistoryUpsertCommand] 매핑 후
 *   [DailySalesHistoryUpsertService.upsert] 호출
 * - 도메인 결과 [DailySalesHistoryUpsertResult] → 청크 status / failure 집계
 * - 청크 commit 실패 시 청크 전체 failed 처리
 * - [SapInboundAuditService] 감사 기록 (chunks 수 포함)
 *
 * 트랜잭션 경계는 [ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 * 어댑터 자체는 `@Transactional` 을 부착하지 않으며 (audit 가 commit 후 기록되어야 함), 도메인 서비스도 helper 의 트랜잭션 안에서 호출된다.
 */
@Service
class SapDailySalesHistoryService(
    private val dailySalesHistoryUpsertService: DailySalesHistoryUpsertService,
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
                val result = chunkedUpsertHelper.processChunk(chunk) { rows ->
                    val commands = rows.map { it.toCommand() }
                    val domainResult = dailySalesHistoryUpsertService.upsert(commands)
                    ChunkProcessResult(
                        successCount = domainResult.successCount,
                        failures = domainResult.failures.map { FailureItem(it.identifier, it.reason) }
                    )
                }
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

    private fun DailySalesHistoryRequestItem.toCommand(): DailySalesHistoryUpsertCommand =
        DailySalesHistoryUpsertCommand(
            sapAccountCode = sapAccountCode,
            salesDate = salesDate,
            erpSalesAmount1 = erpSalesAmount1,
            erpSalesAmount2 = erpSalesAmount2,
            erpSalesAmount3 = erpSalesAmount3,
            erpDistributionAmount1 = erpDistributionAmount1,
            erpDistributionAmount2 = erpDistributionAmount2,
            erpDistributionAmount3 = erpDistributionAmount3,
            ledgerAmount = ledgerAmount
        )

    private fun externalKey(item: DailySalesHistoryRequestItem): String? {
        val ac = item.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val sd = item.salesDate?.takeIf { it.isNotBlank() } ?: return null
        return ac + sd
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
}
