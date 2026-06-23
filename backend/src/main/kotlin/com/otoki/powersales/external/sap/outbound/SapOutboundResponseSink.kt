package com.otoki.powersales.external.sap.outbound

import com.otoki.powersales.external.common.outboundlog.OutboundResponseSink
import com.otoki.powersales.external.sap.outbound.guard.SapResponseHtmlGuard
import com.otoki.powersales.external.sap.outbound.guard.SapResponseInterpreter
import com.otoki.powersales.external.sap.outbound.service.SapOutboundLogService
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

/**
 * SAP outbound 호출 결과를 `sap_outbound_log` 에 적재하는 [OutboundResponseSink] 구현.
 *
 * 범용 [com.otoki.powersales.external.common.outboundlog.ExternalApiLogInterceptor] 가 SAP RestClient
 * 호출 시 본 sink 로 응답을 위임한다. 인터셉터를 SAP 전용으로 따로 두지 않고, 범용 인터셉터 1개가
 * 응답 본문을 1회만 buffering 하도록 통일하기 위한 분리다.
 *
 * - **interface_id / endpoint_path**: uri 의 마지막 path segment(= `SDxxxxx`).
 * - **request_count**: 요청 JSON 의 `request` / `REQUEST` 배열 크기. 배열이 없으면 1(단건).
 * - **result_code / result_msg**: 응답 JSON 파싱([SapResponseInterpreter]). HTML 응답이면 INVALID_RESPONSE.
 * - **attempt_count**: HTTP 호출 1회 = 1 (재시도/큐는 호출 주체 책임).
 *
 * 빈 페이지 SKIP(HTTP 호출 없이 기록되는 row) 처럼 실제 송신이 없는 이력은 인터셉터/sink 가 관측할
 * 수 없으므로 해당 sender 가 직접 [SapOutboundLogService] 로 적재한다.
 */
class SapOutboundResponseSink(
    private val logService: SapOutboundLogService,
    private val objectMapper: ObjectMapper,
) : OutboundResponseSink {

    private val log = LoggerFactory.getLogger(SapOutboundResponseSink::class.java)

    override fun accept(
        uri: String,
        requestBody: String,
        httpStatus: Int?,
        responseBody: String?,
        requestedAt: LocalDateTime,
        durationMs: Long,
        networkError: Boolean,
    ) {
        val interfaceId = lastPathSegment(uri)
        val endpointPath = "/$interfaceId"
        val requestCount = countRequestRows(requestBody)

        val (resultCode, resultMsg, errorDetail) = when {
            networkError -> Triple(RESULT_CODE_FAIL, "NETWORK_ERROR", null)
            !SapResponseHtmlGuard.isValid(responseBody) ->
                Triple(RESULT_CODE_INVALID_RESPONSE, "HTML_RESPONSE_DETECTED", responseBody?.take(MAX_ERROR_DETAIL_LENGTH))
            else -> {
                val interpreted = SapResponseInterpreter.interpret(objectMapper, responseBody)
                val code = if (interpreted.success) RESULT_CODE_SUCCESS else RESULT_CODE_FAIL
                Triple(code, interpreted.message, null)
            }
        }

        try {
            logService.log(
                interfaceId = interfaceId,
                endpointPath = endpointPath,
                requestCount = requestCount,
                httpStatus = httpStatus,
                resultCode = resultCode,
                resultMsg = resultMsg?.take(MAX_RESULT_MSG_LENGTH),
                attemptCount = 1,
                durationMs = durationMs,
                errorDetail = errorDetail,
                requestedAt = requestedAt,
                completedAt = LocalDateTime.now(),
            )
        } catch (ex: Exception) {
            log.error("SAP outbound 송신 이력 저장 실패 interfaceId=$interfaceId", ex)
        }
    }

    /** 요청 JSON 의 `request` / `REQUEST` 배열 크기. 배열이 없으면 1(단건 호출). */
    private fun countRequestRows(requestBody: String): Int {
        if (requestBody.isBlank()) return 1
        return try {
            val node = objectMapper.readTree(requestBody)
            val arrayNode = node["request"] ?: node["REQUEST"]
            if (arrayNode != null && arrayNode.isArray) arrayNode.size() else 1
        } catch (_: Exception) {
            1
        }
    }

    private fun lastPathSegment(uri: String): String {
        val path = uri.substringBefore('?').substringBefore('#').trimEnd('/')
        return path.substringAfterLast('/')
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
