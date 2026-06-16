package com.otoki.powersales.external.common.outboundlog

import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.StreamUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime

/**
 * 외부 HTTP outbound 호출을 [ExternalApiLog] 로 자동 적재하는 공통 인터셉터.
 *
 * SAP / SF / Naver 의 각 `RestClient` builder 에 `target` 만 달리해 등록한다.
 * 등록된 RestClient 의 모든 호출(성공/실패/네트워크 예외)을 1건당 1 row 로 기록한다.
 *
 * 설계 원칙:
 * - **에러도 빠짐없이 기록** — 2xx 뿐 아니라 4xx/5xx 응답, 네트워크 예외(연결 실패/타임아웃)까지
 *   모두 1 row 로 남긴다. 네트워크 예외는 status 없이 success=false 로 기록 후 원 예외를 rethrow.
 * - **본문 캡처는 [captureBody] 가 true 일 때만** (local / dev profile). 본문을 읽으려면 응답 stream 을
 *   소비해야 하는데, 그대로 두면 다운스트림 컨버터가 본문을 다시 읽지 못한다. 따라서 캡처 시에는
 *   응답 본문을 메모리로 buffering 한 [BufferingClientHttpResponse] 로 감싸 재전달한다.
 *   prod 는 PII/용량 보호를 위해 본문을 읽지 않고 status / 소요시간 / 예외만 기록한다.
 * - **best-effort** — 로그 적재 실패가 실제 외부 호출을 막지 않도록 try/catch 로 격리한다.
 */
class ExternalApiLogInterceptor(
    private val target: String,
    private val logService: ExternalApiLogService,
    private val captureBody: Boolean = false
) : ClientHttpRequestInterceptor {

    private val log = LoggerFactory.getLogger(ExternalApiLogInterceptor::class.java)

    override fun intercept(
        request: org.springframework.http.HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val requestedAt = LocalDateTime.now()
        val startNanos = System.nanoTime()
        val method = request.method.name()
        val uri = request.uri.toString()
        val requestBody = if (captureBody) decodeBody(body) else null

        try {
            val response = execution.execute(request, body)
            val status = runCatching { response.statusCode.value() }.getOrNull()
            val success = status != null && status in 200..299

            // 본문 캡처 시에만 응답 stream 을 읽고 buffering wrapper 로 다시 감싸 다운스트림에 전달한다.
            val (responseBody, returned) = if (captureBody) bufferResponse(response) else (null to response)

            record(
                method = method,
                uri = uri,
                httpStatus = status,
                success = success,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = if (success) null else "HTTP $status",
                requestBody = requestBody,
                responseBody = responseBody
            )
            return returned
        } catch (ex: IOException) {
            record(
                method = method,
                uri = uri,
                httpStatus = null,
                success = false,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}",
                requestBody = requestBody,
                responseBody = null
            )
            throw ex
        } catch (ex: RuntimeException) {
            record(
                method = method,
                uri = uri,
                httpStatus = null,
                success = false,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}",
                requestBody = requestBody,
                responseBody = null
            )
            throw ex
        }
    }

    /**
     * 응답 본문을 메모리로 읽어 캡처하고, 동일 본문을 다시 제공하는 [BufferingClientHttpResponse] 를 반환한다.
     * 본문 읽기 실패 시에도 원 응답을 그대로 돌려보내 호출이 깨지지 않게 한다.
     */
    private fun bufferResponse(response: ClientHttpResponse): Pair<String?, ClientHttpResponse> {
        return try {
            val bytes = StreamUtils.copyToByteArray(response.body)
            val text = String(bytes, StandardCharsets.UTF_8)
            text to BufferingClientHttpResponse(response, bytes)
        } catch (ex: Exception) {
            log.warn("외부 API 응답 본문 캡처 실패 target={} (로그는 본문 없이 적재)", target, ex)
            null to response
        }
    }

    private fun decodeBody(body: ByteArray): String? =
        if (body.isEmpty()) null else String(body, StandardCharsets.UTF_8)

    private fun record(
        method: String,
        uri: String,
        httpStatus: Int?,
        success: Boolean,
        startNanos: Long,
        requestedAt: LocalDateTime,
        errorDetail: String?,
        requestBody: String?,
        responseBody: String?
    ) {
        try {
            logService.log(
                targetSystem = target,
                endpointKey = ExternalApiEndpointKeyResolver.resolve(target, uri),
                httpMethod = method,
                uri = uri,
                httpStatus = httpStatus,
                success = success,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now(),
                requestBody = requestBody,
                responseBody = responseBody
            )
        } catch (ex: Exception) {
            log.error("외부 API 호출 로그 적재 실패 target={} method={} uri={}", target, method, uri, ex)
        }
    }

    /**
     * 이미 메모리로 읽은 본문([bytes])을 반복 제공하는 [ClientHttpResponse] 래퍼.
     * 인터셉터가 본문을 캡처하면서도 다운스트림 컨버터가 동일 본문을 다시 읽을 수 있도록 한다.
     */
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
}
