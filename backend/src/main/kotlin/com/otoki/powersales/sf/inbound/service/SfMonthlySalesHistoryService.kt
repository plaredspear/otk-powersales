package com.otoki.powersales.sf.inbound.service

import com.otoki.powersales.sales.service.MonthlySalesHistoryUpsertService
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand
import com.otoki.powersales.sf.auth.audit.SfInboundAccepted
import com.otoki.powersales.sf.inbound.dto.sales.SfChunkResult
import com.otoki.powersales.sf.inbound.dto.sales.SfFailureItem
import com.otoki.powersales.sf.inbound.dto.sales.SfMonthlySalesHistoryRequestItem
import com.otoki.powersales.sf.inbound.dto.sales.SfSalesHistoryDetail
import com.otoki.powersales.sf.inbound.exception.SfPayloadTooLargeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * SF 월 매출 이력 인바운드 어댑터 (Spec #775).
 *
 * 책임:
 * - SF 페이로드 size 한도 검증 ([SfPayloadTooLargeException])
 * - 청크 분할 + 청크 단위 [SfChunkedUpsertHelper] 트랜잭션 격리
 * - 청크별로 페이로드 → 도메인 커맨드 [MonthlySalesHistoryUpsertCommand] 매핑 후
 *   [MonthlySalesHistoryUpsertService.upsert] 호출 (SAP / SF 공유 도메인 service)
 * - 도메인 결과 → 청크 status / failure 집계
 * - 청크 commit 실패 시 청크 전체 failed 처리
 *
 * `REQUEST_ACCEPTED` audit 기록 (chunks 수 포함) 은 [com.otoki.powersales.sf.auth.audit.SfInboundAuditAspect]
 * 가 `@SfInboundAccepted` annotation 을 트리거로 공통 처리한다.
 *
 * 트랜잭션 경계는 [SfChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 */
@Service
class SfMonthlySalesHistoryService(
    private val monthlySalesHistoryUpsertService: MonthlySalesHistoryUpsertService,
    private val chunkedUpsertHelper: SfChunkedUpsertHelper,
    @Value("\${sf.inbound.sales.chunk-size:1000}") private val chunkSize: Int,
    @Value("\${sf.inbound.sales.max-rows:50000}") private val maxRows: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SfInboundAccepted("items", reasonTemplate = "success={success} failure={failure} chunks={chunks}")
    fun upsert(items: List<SfMonthlySalesHistoryRequestItem>): SfSalesHistoryDetail {
        if (items.size > maxRows) {
            throw SfPayloadTooLargeException(maxRows, items.size)
        }

        val chunks = items.chunked(chunkSize)
        val chunkResults = mutableListOf<SfChunkResult>()
        val allFailures = mutableListOf<SfFailureItem>()
        var totalSuccess = 0

        chunks.forEachIndexed { idx, chunk ->
            try {
                val result = chunkedUpsertHelper.processChunk(chunk) { rows ->
                    val commands = rows.map { it.toCommand() }
                    val domainResult = monthlySalesHistoryUpsertService.upsert(commands)
                    SfChunkProcessResult(
                        successCount = domainResult.successCount,
                        failures = domainResult.failures.map { SfFailureItem(it.identifier, it.reason) }
                    )
                }
                allFailures += result.failures
                totalSuccess += result.successCount
                val status = when {
                    result.failures.isEmpty() -> SfChunkResult.STATUS_SUCCESS
                    result.successCount == 0 -> SfChunkResult.STATUS_FAILED
                    else -> SfChunkResult.STATUS_PARTIAL
                }
                val reason = if (result.failures.isNotEmpty()) "${result.failures.size} rows failed validation" else null
                chunkResults += SfChunkResult(idx, status, chunk.size, reason)
            } catch (ex: Exception) {
                log.error("SF monthly sales chunk {} commit failed: {}", idx, ex.message, ex)
                val reason = "chunk commit failed: ${ex.message?.take(150)}"
                chunkResults += SfChunkResult(idx, SfChunkResult.STATUS_FAILED, chunk.size, reason)
                chunk.forEach { item ->
                    allFailures += SfFailureItem(externalKey(item), reason)
                }
            }
        }

        return SfSalesHistoryDetail(
            successCount = totalSuccess,
            failureCount = allFailures.size,
            failures = allFailures,
            chunks = chunkResults
        )
    }

    private fun SfMonthlySalesHistoryRequestItem.toCommand(): MonthlySalesHistoryUpsertCommand =
        MonthlySalesHistoryUpsertCommand(
            sapAccountCode = sapAccountCode,
            salesYearMonth = salesYearMonth,
            abcClosingAmount1 = abcClosingAmount1,
            abcClosingAmount2 = abcClosingAmount2,
            abcClosingAmount3 = abcClosingAmount3,
            totalLedgerAmount = totalLedgerAmount,
            shipClosingAmount = shipClosingAmount,
            rlsales = rlsales
        )

    private fun externalKey(item: SfMonthlySalesHistoryRequestItem): String? {
        val ac = item.sapAccountCode?.takeIf { it.isNotBlank() } ?: return null
        val ym = item.salesYearMonth?.takeIf { it.isNotBlank() } ?: return null
        return ac + ym
    }
}
