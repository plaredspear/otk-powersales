package com.otoki.powersales.sap.outbound.client

import com.otoki.powersales.sap.outbound.config.SapOutboundProperties
import com.otoki.powersales.sap.outbound.dto.SapOutboundRequest
import com.otoki.powersales.sap.outbound.dto.SapOutboundResponse
import com.otoki.powersales.sap.outbound.exception.SapOutboundException
import com.otoki.powersales.sap.outbound.service.SapOutboundLogService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.LocalDateTime

@Component
class SapOutboundClientImpl(
    @Qualifier("sapOutboundRestClient") private val restClient: RestClient,
    private val properties: SapOutboundProperties,
    private val logService: SapOutboundLogService
) : SapOutboundClient {

    private val log = LoggerFactory.getLogger(SapOutboundClientImpl::class.java)

    override fun send(path: String, request: SapOutboundRequest<*>): SapOutboundResponse {
        if (properties.baseUrl.isBlank()) {
            throw IllegalStateException("sap.outbound.base-url 이 설정되지 않았습니다")
        }

        val requestedAt = LocalDateTime.now()
        val startNanos = System.nanoTime()
        val requestCount = request.reqItemList.size

        if (requestCount == 0) {
            val response = SapOutboundResponse(
                resultCode = SapOutboundResponse.SUCCESS_CODE,
                resultMsg = "SKIPPED_EMPTY"
            )
            persistLog(
                interfaceId = request.interfaceId,
                endpointPath = path,
                requestCount = 0,
                httpStatus = null,
                response = response,
                attemptCount = 0,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = null
            )
            return response
        }

        val maxAttempts = properties.retry.maxAttempts.coerceAtLeast(1)
        val delayMs = properties.retry.delayMs.coerceAtLeast(0)
        var attempt = 0
        var lastException: Throwable? = null
        var lastHttpStatus: Int? = null
        var lastErrorDetail: String? = null

        while (attempt < maxAttempts) {
            attempt += 1
            try {
                val httpResponse = restClient.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .toEntity(SapOutboundResponse::class.java)

                val body = httpResponse.body
                    ?: throw SapOutboundException("SAP 응답 본문이 비어 있습니다")

                persistLog(
                    interfaceId = request.interfaceId,
                    endpointPath = path,
                    requestCount = requestCount,
                    httpStatus = httpResponse.statusCode.value(),
                    response = body,
                    attemptCount = attempt,
                    startNanos = startNanos,
                    requestedAt = requestedAt,
                    errorDetail = null
                )
                return body
            } catch (ex: HttpClientErrorException) {
                lastException = ex
                lastHttpStatus = ex.statusCode.value()
                lastErrorDetail = "HTTP ${ex.statusCode.value()} ${ex.statusText} body=${ex.responseBodyAsString.take(MAX_ERROR_DETAIL_LENGTH)}"
                log.warn("SAP 송신 실패 (4xx, 재시도 없음) interfaceId=${request.interfaceId} path=$path status=${ex.statusCode}")
                break
            } catch (ex: HttpServerErrorException) {
                lastException = ex
                lastHttpStatus = ex.statusCode.value()
                lastErrorDetail = "HTTP ${ex.statusCode.value()} ${ex.statusText} body=${ex.responseBodyAsString.take(MAX_ERROR_DETAIL_LENGTH)}"
                log.warn("SAP 송신 실패 (5xx) interfaceId=${request.interfaceId} path=$path status=${ex.statusCode} attempt=$attempt/$maxAttempts")
                if (attempt < maxAttempts) sleepBeforeRetry(delayMs)
            } catch (ex: ResourceAccessException) {
                lastException = ex
                lastHttpStatus = null
                lastErrorDetail = "${ex.javaClass.simpleName}: ${ex.message}".take(MAX_ERROR_DETAIL_LENGTH)
                log.warn("SAP 송신 실패 (네트워크) interfaceId=${request.interfaceId} path=$path attempt=$attempt/$maxAttempts cause=${ex.message}")
                if (attempt < maxAttempts) sleepBeforeRetry(delayMs)
            }
        }

        val cause = lastException
            ?: SapOutboundException("SAP 송신이 알 수 없는 사유로 실패했습니다")

        persistLog(
            interfaceId = request.interfaceId,
            endpointPath = path,
            requestCount = requestCount,
            httpStatus = lastHttpStatus,
            response = null,
            attemptCount = attempt,
            startNanos = startNanos,
            requestedAt = requestedAt,
            errorDetail = lastErrorDetail
        )

        throw SapOutboundException(
            detail = lastErrorDetail ?: cause.message ?: "SAP 송신 실패",
            httpStatusCode = lastHttpStatus,
            cause = cause
        )
    }

    private fun sleepBeforeRetry(delayMs: Long) {
        if (delayMs <= 0) return
        try {
            Thread.sleep(delayMs)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun persistLog(
        interfaceId: String,
        endpointPath: String,
        requestCount: Int,
        httpStatus: Int?,
        response: SapOutboundResponse?,
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
                resultCode = response?.resultCode,
                resultMsg = response?.resultMsg,
                attemptCount = attemptCount,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now()
            )
        } catch (ex: Exception) {
            log.error("SAP 송신 이력 저장 실패 interfaceId=$interfaceId path=$endpointPath", ex)
        }
    }

    companion object {
        private const val MAX_ERROR_DETAIL_LENGTH = 4000
    }
}
