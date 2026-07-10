package com.otoki.powersales.external.sf.outbound

import com.otoki.powersales.external.common.outboundlog.OutboundSuccessVerdict
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * SF Apex REST 응답 본문(JSON)에서 도메인 성공/실패를 판정하는 유틸.
 *
 * SF 는 클레임 등록 등 write 계열 endpoint 의 도메인 실패도 HTTP 200 으로 응답하고, 실제 성공 여부를
 * `RESULT_CODE` 로 전달한다([SfApiResponse.isSuccess] — 성공은 `"200"`). 따라서 [ExternalApiLogInterceptor] 의
 * HTTP status 판정만으로는 `external_api_log.success` 가 항상 SUCCESS 로 잘못 기록된다. 이 extractor 를 success
 * resolver 로 주입해 `RESULT_CODE` 기반으로 판정을 override 한다.
 *
 * **`RESULT_CODE` 가 있는 응답에만 판정한다.** fetch 계열(배열/목록) 응답, OAuth 토큰 응답 등 `RESULT_CODE` 가
 * 없는 응답은 null 을 반환해 기존 HTTP status 판정을 그대로 유지한다 — write 계열 `{RESULT_CODE, RESULT_MSG}`
 * 응답에만 body 판정이 적용된다. 파싱 실패도 null(HTTP 판정 fallback).
 */
@Component
class SfResponseSuccessExtractor(
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @param httpStatus 원 응답 HTTP status. non-2xx 는 HTTP 판정에 맡기고 null 반환(중복 판정 회피).
     * @param responseBody 응답 본문.
     * @return `RESULT_CODE` 를 담은 응답이면 성공/실패 판정, 아니면 null.
     */
    fun resolve(httpStatus: Int?, responseBody: String?): OutboundSuccessVerdict? {
        // HTTP 자체가 실패(non-2xx)면 그 판정을 존중하고 body 재판정을 건너뛴다.
        if (httpStatus == null || httpStatus !in 200..299) return null
        if (responseBody.isNullOrBlank()) return null
        return try {
            val node = objectMapper.readTree(responseBody)
            val resultCode = node["RESULT_CODE"]?.asString() ?: return null
            val success = resultCode == SF_SUCCESS_CODE
            if (success) {
                OutboundSuccessVerdict(success = true)
            } else {
                val msg = node["RESULT_MSG"]?.asString()?.takeIf { it.isNotBlank() }
                OutboundSuccessVerdict(
                    success = false,
                    errorMessage = "RESULT_CODE=$resultCode${msg?.let { ": $it" }.orEmpty()}",
                )
            }
        } catch (ex: Exception) {
            log.debug("[sf-response-success] 성공 판정 파싱 실패 — null(HTTP 판정 유지). error={}", ex.message)
            null
        }
    }

    private companion object {
        /** SF 성공 코드 — [SfApiResponse.isSuccess] 와 동일 규칙. */
        const val SF_SUCCESS_CODE = "200"
    }
}
