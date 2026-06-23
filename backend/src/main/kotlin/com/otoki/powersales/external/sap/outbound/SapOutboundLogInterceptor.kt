package com.otoki.powersales.external.sap.outbound

import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.external.sap.outbound.guard.SapResponseInterpreter
import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.StreamUtils
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime

/**
 * SAP outbound RestClient 의 **모든 실제 HTTP 호출**을 [com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog]
 * 로 1건당 1 row 적재하는 인터셉터.
 *
 * 도입 배경: SAP outbound 송신 sender 가 sender 마다 명시적으로 [SapOutboundLogService] 를 호출하던 구조라,
 * 단건 동기 sender(OrderRequestDetail / InventorySearch / LoanInquiry / OrderRequestCancel) 와 범용
 * outbox 송신(SD03050) 이 `sap_outbound_log` 에 전혀 남지 않는 누락이 있었다. RestClient 레벨 인터셉터로
 * 일괄 기록해 누락을 제거하고, 향후 sender 추가 시에도 자동 포함되게 한다.
 *
 * - **interface_id / endpoint_path**: uri 의 마지막 path segment(= `SDxxxxx`) 를 그대로 사용한다
 *   ([com.otoki.powersales.external.common.outboundlog.ExternalApiEndpointKeyResolver.lastPathSegment] 와 동일 규칙).
 * - **request_count**: 요청 JSON 의 `request` / `REQUEST` 배열 크기. 배열이 없으면 1(단건).
 * - **result_code / result_msg**: 응답 JSON 의 `resultCode` / `resutlMsg`(SAP 오타 필드) 파싱
 *   ([com.otoki.powersales.external.sap.outbox.SapOutboxBatchService.interpretSapResponse] 동등).
 * - **attempt_count**: HTTP 호출 1회 = 1 (재시도/큐는 호출 주체 책임이라 인터셉터는 호출 단위로만 카운트).
 * - **best-effort**: 로그 적재 실패가 실제 SAP 호출을 막지 않도록 try/catch 로 격리한다.
 *
 * 응답 본문을 읽으려면 stream 을 소비해야 하므로, 읽은 본문을 buffering wrapper 로 다시 감싸 다운스트림
 * 컨버터가 동일 본문을 재차 읽을 수 있게 한다 ([com.otoki.powersales.external.common.outboundlog.ExternalApiLogInterceptor]
 * 와 동일 기법).
 *
 * 빈 페이지 SKIP(HTTP 호출 없이 기록되는 row) 처럼 **실제 송신이 일어나지 않는** 이력은 인터셉터가 볼 수
 * 없으므로 해당 sender 가 직접 [SapOutboundLogService] 로 적재한다.
 */
class SapOutboundLogInterceptor(
    private val logService: SapOutboundLogService,
    private val objectMapper: ObjectMapper,
) : ClientHttpRequestInterceptor {

    private val log = LoggerFactory.getLogger(SapOutboundLogInterceptor::class.java)

    override fun intercept(
        request: org.springframework.http.HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val requestedAt = LocalDateTime.now()
        val startNanos = System.nanoTime()
        val uri = request.uri.toString()
        val interfaceId = lastPathSegment(uri)
        val endpointPath = "/$interfaceId"
        val requestCount = countRequestRows(body)

        try {
            val response = execution.execute(request, body)
            val status = runCatching { response.statusCode.value() }.getOrNull()
            val (responseBody, returned) = bufferResponse(response)

            val htmlValid = SapResponseHtmlGuard.isValid(responseBody)
            val (resultCode, resultMsg) = if (htmlValid) {
                interpretResultCode(responseBody)
            } else {
                RESULT_CODE_INVALID_RESPONSE to "HTML_RESPONSE_DETECTED"
            }

            record(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = status,
                resultCode = resultCode,
                resultMsg = resultMsg,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = if (htmlValid) null else responseBody?.take(MAX_ERROR_DETAIL_LENGTH),
            )
            return returned
        } catch (ex: IOException) {
            record(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = null,
                resultCode = RESULT_CODE_FAIL,
                resultMsg = "NETWORK_ERROR",
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}".take(MAX_ERROR_DETAIL_LENGTH),
            )
            throw ex
        } catch (ex: RuntimeException) {
            record(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = null,
                resultCode = RESULT_CODE_FAIL,
                resultMsg = ex.javaClass.simpleName,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}".take(MAX_ERROR_DETAIL_LENGTH),
            )
            throw ex
        }
    }

    /** 응답 본문을 메모리로 읽어 캡처하고, 동일 본문을 다시 제공하는 wrapper 를 반환한다. */
    private fun bufferResponse(response: ClientHttpResponse): Pair<String?, ClientHttpResponse> {
        return try {
            val bytes = StreamUtils.copyToByteArray(response.body)
            String(bytes, StandardCharsets.UTF_8) to BufferingClientHttpResponse(response, bytes)
        } catch (ex: Exception) {
            log.warn("SAP outbound 응답 본문 캡처 실패 (로그는 본문 없이 적재)", ex)
            null to response
        }
    }

    /** 요청 JSON 의 `request` / `REQUEST` 배열 크기. 배열이 없으면 1(단건 호출). */
    private fun countRequestRows(body: ByteArray): Int {
        if (body.isEmpty()) return 1
        return try {
            val node = objectMapper.readTree(String(body, StandardCharsets.UTF_8))
            val arrayNode = node["request"] ?: node["REQUEST"]
            if (arrayNode != null && arrayNode.isArray) arrayNode.size() else 1
        } catch (_: Exception) {
            1
        }
    }

    /**
     * 공통 파서([SapResponseInterpreter]) 결과를 `sap_outbound_log.result_code` 어휘(SUCCESS/FAIL)로 정규화한다.
     * 본문 파싱 불가(resultCode 누락)면 코드 미상(null)으로 둔다.
     */
    private fun interpretResultCode(body: String?): Pair<String?, String?> {
        val result = SapResponseInterpreter.interpret(objectMapper, body)
        val code = when {
            result.success -> RESULT_CODE_SUCCESS
            result.resultCodeRaw != null -> RESULT_CODE_FAIL
            // resultCode 자체가 없던 케이스(빈 본문/파싱 실패)는 명시적으로 FAIL 로 기록.
            else -> RESULT_CODE_FAIL
        }
        return code to result.message
    }

    private fun lastPathSegment(uri: String): String {
        val path = uri.substringBefore('?').substringBefore('#').trimEnd('/')
        return path.substringAfterLast('/')
    }

    private fun record(
        interfaceId: String,
        endpointPath: String,
        requestCount: Int,
        httpStatus: Int?,
        resultCode: String?,
        resultMsg: String?,
        startNanos: Long,
        requestedAt: LocalDateTime,
        errorDetail: String?,
    ) {
        try {
            logService.log(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = httpStatus,
                resultCode = resultCode,
                resultMsg = resultMsg?.take(MAX_RESULT_MSG_LENGTH),
                attemptCount = 1,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now(),
            )
        } catch (ex: Exception) {
            log.error("SAP outbound 송신 이력 저장 실패 interfaceId=$interfaceId", ex)
        }
    }

    /** 이미 메모리로 읽은 본문을 반복 제공하는 [ClientHttpResponse] 래퍼. */
    private class BufferingClientHttpResponse(
        private val delegate: ClientHttpResponse,
        private val bytes: ByteArray
    ) : ClientHttpResponse {
        override fun getStatusCode(): HttpStatusCode = delegate.statusCode
        override fun getStatusText(): String = delegate.statusText
        override fun getHeaders(): HttpHeaders = delegate.headers
        override fun getBody(): InputStream = ByteArrayInputStream(bytes)
        override fun close() = delegate.close()
    }

    companion object {
        private const val MAX_ERROR_DETAIL_LENGTH = 4000
        // sap_outbound_log.result_msg 컬럼 length 500 정합 (절단).
        private const val MAX_RESULT_MSG_LENGTH = 500

        // sap_outbound_log.result_code 어휘 (length 10 정합). 배치 sender 의 "SUCCESS"/"FAIL" 와 동일.
        private const val RESULT_CODE_SUCCESS = "SUCCESS"
        private const val RESULT_CODE_FAIL = "FAIL"
        private const val RESULT_CODE_INVALID_RESPONSE = "INVALID_RESPONSE"
    }
}
