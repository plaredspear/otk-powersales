package com.otoki.powersales.external.sf.outbound

import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * SF Apex REST 응답 본문(JSON)에서 데이터 레코드 건수를 산출하는 유틸.
 *
 * `external_api_log.response_count` 적재를 위해 [ExternalApiLogInterceptor] 의 count resolver 로 주입된다.
 * SF 응답에서 데이터 배열을 찾는 규칙은 fetch 계열 클라이언트와 공용 [SfResponseArrayExtractor] 를 그대로 사용 —
 * 최상위가 배열이면 그대로, 객체면 흔한 wrapper key(`Result` 등)를 대소문자 무시로 탐색.
 *
 * 데이터 배열을 찾지 못하는 응답(등록/전송 성공 `{RESULT_CODE, RESULT_MSG}` 단일 형식, OAuth 토큰 응답 등)
 * 은 null 을 반환한다 — "목록을 회수한 호출" 에서만 건수가 기록된다.
 */
@Component
class SfResponseCountExtractor(
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 응답 본문에서 데이터 레코드 배열 크기를 반환. 배열을 찾지 못하거나 파싱 실패 시 null. */
    fun extract(responseBody: String?): Int? {
        if (responseBody.isNullOrBlank()) return null
        return try {
            val arrayNode = SfResponseArrayExtractor.extractArrayNode(objectMapper.readTree(responseBody)) ?: return null
            arrayNode.size()
        } catch (ex: Exception) {
            log.debug("[sf-response-count] 응답 건수 산출 파싱 실패 — null. error={}", ex.message)
            null
        }
    }
}
