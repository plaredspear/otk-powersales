package com.otoki.powersales.external.common.outboundlog

import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import org.slf4j.LoggerFactory
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime

/**
 * 외부 HTTP outbound 호출을 [ExternalApiLog] 로 자동 적재하는 공통 인터셉터.
 *
 * SAP / SF / Naver 의 각 `RestClient` builder 에 `target` 만 달리해 등록한다.
 * 등록된 RestClient 의 모든 호출(성공/실패/네트워크 예외)을 1건당 1 row 로 기록한다.
 *
 * 설계 원칙:
 * - **응답 본문을 읽지 않는다** — 인터셉터 단계에서 body stream 을 소비하면 다운스트림 컨버터가
 *   본문을 다시 읽지 못한다. 따라서 status code / 소요시간 / 예외만 기록하고, 본문 파싱은
 *   기존 sender 의 책임으로 남긴다.
 * - **best-effort** — 로그 적재 실패가 실제 외부 호출을 막지 않도록 try/catch 로 격리한다.
 * - 네트워크 예외(연결 실패/타임아웃)는 status 없이 success=false 로 기록 후 원 예외를 그대로 rethrow.
 */
class ExternalApiLogInterceptor(
    private val target: String,
    private val logService: ExternalApiLogService
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

        try {
            val response = execution.execute(request, body)
            val status = runCatching { response.statusCode.value() }.getOrNull()
            val success = status != null && status in 200..299
            record(
                method = method,
                uri = uri,
                httpStatus = status,
                success = success,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = if (success) null else "HTTP $status"
            )
            return response
        } catch (ex: IOException) {
            record(
                method = method,
                uri = uri,
                httpStatus = null,
                success = false,
                startNanos = startNanos,
                requestedAt = requestedAt,
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}"
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
                errorDetail = "${ex.javaClass.simpleName}: ${ex.message}"
            )
            throw ex
        }
    }

    private fun record(
        method: String,
        uri: String,
        httpStatus: Int?,
        success: Boolean,
        startNanos: Long,
        requestedAt: LocalDateTime,
        errorDetail: String?
    ) {
        try {
            logService.log(
                targetSystem = target,
                httpMethod = method,
                uri = uri,
                httpStatus = httpStatus,
                success = success,
                durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now()
            )
        } catch (ex: Exception) {
            log.error("외부 API 호출 로그 적재 실패 target={} method={} uri={}", target, method, uri, ex)
        }
    }
}
