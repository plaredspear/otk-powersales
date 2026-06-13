package com.otoki.powersales.external.sap.outbound.sender

import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import com.otoki.powersales.schedule.sap.DisplayMasterSapPayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.LocalDateTime

/**
 * 진열 마스터(DISPLAY) SAP 송신 sender (Spec #588 P2-B).
 *
 * 페이지 단위로 SAP REST Adapter 에 POST 한다.
 * - interfaceId: [SapConstants.SAP_INTERFACE_DISPLAY_MASTER]
 * - 응답 검증: [SapResponseHtmlGuard]
 * - 결과는 [SapOutboundLogService] 로 적재
 * - **재시도 없음** (#588 §8 Q4)
 */
@Component
class DisplayMasterSapSender(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val logService: SapOutboundLogService
) {

    private val log = LoggerFactory.getLogger(DisplayMasterSapSender::class.java)

    fun sendPage(payload: DisplayMasterSapPayload): Boolean {
        val interfaceId = SapConstants.SAP_INTERFACE_DISPLAY_MASTER
        val endpointPath = "/$interfaceId"
        val requestedAt = LocalDateTime.now()
        val startNanos = System.nanoTime()
        val requestCount = payload.request.size

        if (requestCount == 0) {
            persistLog(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = 0,
                httpStatus = null,
                resultCode = "SKIPPED",
                resultMsg = "EMPTY_PAGE",
                attemptCount = 0,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = null
            )
            return true
        }

        return try {
            val response = restClient.post()
                .uri(endpointPath)
                .body(payload)
                .retrieve()
                .toEntity(String::class.java)

            val status = response.statusCode.value()
            val body = response.body
            val isValid = SapResponseHtmlGuard.isValid(body)

            persistLog(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = status,
                resultCode = if (isValid) "SUCCESS" else "INVALID_RESPONSE",
                resultMsg = if (isValid) null else "HTML_RESPONSE_DETECTED",
                attemptCount = 1,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = if (isValid) null else body?.take(MAX_ERROR_DETAIL_LENGTH)
            )
            if (!isValid) {
                log.warn("SAP 진열 마스터 송신 실패 (HTML 응답) interfaceId={} status={} bodyHead={}", interfaceId, status, body?.take(80))
            }
            isValid
        } catch (ex: HttpStatusCodeException) {
            persistLog(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = ex.statusCode.value(),
                resultCode = "FAIL",
                resultMsg = "HTTP_${ex.statusCode.value()}",
                attemptCount = 1,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = ex.responseBodyAsString.take(MAX_ERROR_DETAIL_LENGTH)
            )
            log.warn("SAP 진열 마스터 송신 실패 (HTTP {}) interfaceId={}", ex.statusCode, interfaceId)
            false
        } catch (ex: ResourceAccessException) {
            persistLog(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = null,
                resultCode = "FAIL",
                resultMsg = "NETWORK_ERROR",
                attemptCount = 1,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}".take(MAX_ERROR_DETAIL_LENGTH)
            )
            log.warn("SAP 진열 마스터 송신 실패 (네트워크) interfaceId={} cause={}", interfaceId, ex.message)
            false
        }
    }

    private fun persistLog(
        interfaceId: String,
        endpointPath: String,
        requestCount: Int,
        httpStatus: Int?,
        resultCode: String?,
        resultMsg: String?,
        attemptCount: Int,
        startNanos: Long,
        requestedAt: LocalDateTime,
        errorDetail: String?
    ) {
        try {
            logService.log(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = httpStatus,
                resultCode = resultCode,
                resultMsg = resultMsg,
                attemptCount = attemptCount,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now()
            )
        } catch (ex: Exception) {
            log.error("SAP 진열 마스터 송신 이력 저장 실패 interfaceId=$interfaceId", ex)
        }
    }

    companion object {
        private const val MAX_ERROR_DETAIL_LENGTH = 4000
    }
}
