package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoDetail
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoRequestItem
import com.otoki.powersales.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.service.AttendInfoInsertService
import com.otoki.powersales.schedule.service.dto.AttendInfoInsertCommand
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 출근 정보 인바운드 어댑터. (Spec #562 + #553 / 어댑터-도메인 분리: #635 P2-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 size 한도 검증 ([SapPayloadTooLargeException])
 * - 청크 분할 + 청크 단위 [ChunkedUpsertHelper] 트랜잭션 격리
 * - 청크별로 페이로드 → 도메인 커맨드 [AttendInfoInsertCommand] 매핑 후 [AttendInfoInsertService.insert] 호출
 * - 도메인 적재 결과 → 청크 status / failure 집계
 * - 청크 commit 후 [AttendInfoToScheduleConverter] 호출 (Schedule 변환 트리거)
 * - [SapInboundAuditService] 감사 기록 — 본 어댑터는 audit 을 다음 종류로 기록한다:
 *   1. `REQUEST_ACCEPTED` (success/failure/chunks 집계) — [SapInboundAuditAspect] 가 AOP 로 처리 (#639)
 *   2. `SCHEDULE_CONVERSION` (변환 성공 시 합산 결과) — 본 어댑터 직접 호출 (도메인 데이터 의존 reason)
 *   3. `SCHEDULE_CONVERSION_FAILED` (변환 실패 청크별 사유) — 본 어댑터 직접 호출
 *
 * 트랜잭션 경계는 [ChunkedUpsertHelper] (`REQUIRES_NEW`) 가 청크 단위로 부여한다.
 * 후처리 (`AttendInfoToScheduleConverter`) 도 어댑터 책임 — 도메인이 SAP 인입 후처리에 결합되지 않도록.
 */
@Service
class SapAttendInfoService(
    private val attendInfoInsertService: AttendInfoInsertService,
    private val chunkedUpsertHelper: ChunkedUpsertHelper,
    private val auditService: SapInboundAuditService,
    private val scheduleConverter: AttendInfoToScheduleConverter,
    @Value("\${sap.inbound.attendance.chunk-size:1000}") private val chunkSize: Int,
    @Value("\${sap.inbound.attendance.max-rows:50000}") private val maxRows: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SapInboundAccepted("items", reasonTemplate = "success={success} failure={failure} chunks={chunks}")
    fun insert(items: List<AttendInfoRequestItem>): AttendInfoDetail {
        if (items.size > maxRows) {
            throw SapPayloadTooLargeException(maxRows, items.size)
        }

        val chunks = items.chunked(chunkSize)
        val chunkResults = mutableListOf<ChunkResult>()
        val allFailures = mutableListOf<FailureItem>()
        var totalSuccess = 0
        var aggregatedSummary = ScheduleConversionSummary.ZERO
        var converterCalled = false
        var converterSucceeded = false

        chunks.forEachIndexed { idx, chunk ->
            val savedThisChunk = mutableListOf<AttendInfo>()
            try {
                val result = chunkedUpsertHelper.processChunk(chunk) { rows ->
                    val commands = rows.map { it.toCommand() }
                    val domainResult = attendInfoInsertService.insert(commands)
                    savedThisChunk += domainResult.savedAttendInfos
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
                log.error("AttendInfo chunk {} commit failed: {}", idx, ex.message, ex)
                val reason = "chunk commit failed: ${ex.message?.take(150)}"
                chunkResults += ChunkResult(idx, ChunkResult.STATUS_FAILED, chunk.size, reason)
                chunk.forEach { item ->
                    allFailures += FailureItem(identifier(item), reason)
                }
            }

            if (savedThisChunk.isNotEmpty()) {
                converterCalled = true
                try {
                    aggregatedSummary += scheduleConverter.convert(savedThisChunk)
                    converterSucceeded = true
                } catch (ex: Exception) {
                    log.error("AttendInfo schedule conversion chunk {} failed: {}", idx, ex.message, ex)
                    recordScheduleConversionFailed(idx, ex)
                }
            }
        }

        if (converterSucceeded) {
            recordScheduleConversion(aggregatedSummary)
        }
        return AttendInfoDetail(
            successCount = totalSuccess,
            failureCount = allFailures.size,
            failures = allFailures,
            chunks = chunkResults,
            scheduleConversion = if (converterCalled) aggregatedSummary else null
        )
    }

    private fun AttendInfoRequestItem.toCommand(): AttendInfoInsertCommand = AttendInfoInsertCommand(
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )

    private fun identifier(item: AttendInfoRequestItem): String? {
        val ec = item.employeeCode?.takeIf { it.isNotBlank() } ?: return null
        val sd = item.startDate?.takeIf { it.isNotBlank() } ?: return ec
        return ec + sd
    }

    private fun recordScheduleConversion(summary: ScheduleConversionSummary) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.SCHEDULE_CONVERSION,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                reason = summary.toReason()
            )
        )
    }

    private fun recordScheduleConversionFailed(chunkIndex: Int, ex: Exception) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        val reason = "chunk=$chunkIndex error=${ex.message?.take(150)}"
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.SCHEDULE_CONVERSION_FAILED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                reason = reason
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }
}
