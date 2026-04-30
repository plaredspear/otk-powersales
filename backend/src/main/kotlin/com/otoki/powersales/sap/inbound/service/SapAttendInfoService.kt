package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.AttendType
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoDetail
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.schedule.repository.AttendInfoRepository
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
 * SAP 출근 정보 인바운드 INSERT 서비스. (Spec #562)
 *
 * 레거시 `IF_REST_SAP_AttendInfo` 와 동등한 INSERT only 모델 (옵션 C).
 *
 * - 한 호출에 대량 데이터 가능성 → [chunkSize] 단위 청크 분할
 * - 청크 단위 [ChunkedUpsertHelper] 의 REQUIRES_NEW 트랜잭션 처리
 * - AttendType 룩업 ([AttendType.fromCode]) 실패 시 원본 코드 그대로 저장 (D3 결정)
 */
@Service
class SapAttendInfoService(
    private val attendInfoRepository: AttendInfoRepository,
    private val chunkedUpsertHelper: ChunkedUpsertHelper,
    private val auditService: SapInboundAuditService,
    @Value("\${sap.inbound.attendance.chunk-size:1000}") private val chunkSize: Int,
    @Value("\${sap.inbound.attendance.max-rows:50000}") private val maxRows: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun insert(items: List<AttendInfoRequestItem>): AttendInfoDetail {
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
                log.error("AttendInfo chunk {} commit failed: {}", idx, ex.message, ex)
                val reason = "chunk commit failed: ${ex.message?.take(150)}"
                chunkResults += ChunkResult(idx, ChunkResult.STATUS_FAILED, chunk.size, reason)
                chunk.forEach { item ->
                    allFailures += FailureItem(identifier(item), reason)
                }
            }
        }

        recordAccepted(items.size, totalSuccess, allFailures.size, chunks.size)
        return AttendInfoDetail(
            successCount = totalSuccess,
            failureCount = allFailures.size,
            failures = allFailures,
            chunks = chunkResults
        )
    }

    private fun processChunkRows(rows: List<AttendInfoRequestItem>): ChunkProcessResult {
        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<AttendInfo>()

        rows.forEach { item ->
            val employeeCode = item.employeeCode?.takeIf { it.isNotBlank() }
            val startDate = item.startDate?.takeIf { it.isNotBlank() }
            val endDate = item.endDate?.takeIf { it.isNotBlank() }
            val attendType = item.attendType?.takeIf { it.isNotBlank() }

            if (employeeCode == null) {
                failures += FailureItem(null, "EmployeeCode 필수")
                return@forEach
            }
            if (startDate == null) {
                failures += FailureItem(employeeCode, "StartDate 필수")
                return@forEach
            }
            if (endDate == null) {
                failures += FailureItem(employeeCode, "EndDate 필수")
                return@forEach
            }
            if (attendType == null) {
                failures += FailureItem(employeeCode, "AttendType 필수")
                return@forEach
            }
            if (!isValidYyyymmdd(startDate)) {
                failures += FailureItem(employeeCode + startDate, "StartDate YYYYMMDD 형식 오류: $startDate")
                return@forEach
            }
            if (!isValidYyyymmdd(endDate)) {
                failures += FailureItem(employeeCode + endDate, "EndDate YYYYMMDD 형식 오류: $endDate")
                return@forEach
            }

            // AttendType 룩업: 매칭 실패 시 원본 코드 그대로 저장 (D3 결정, 거부하지 않음)
            AttendType.fromCode(attendType)

            toSave += AttendInfo(
                employeeCode = employeeCode,
                startDate = startDate,
                endDate = endDate,
                attendType = attendType,
                status = item.status
            )
        }

        if (toSave.isNotEmpty()) {
            attendInfoRepository.saveAll(toSave)
        }

        return ChunkProcessResult(toSave.size, failures)
    }

    private fun identifier(item: AttendInfoRequestItem): String? {
        val ec = item.employeeCode?.takeIf { it.isNotBlank() } ?: return null
        val sd = item.startDate?.takeIf { it.isNotBlank() } ?: return ec
        return ec + sd
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
    }
}
