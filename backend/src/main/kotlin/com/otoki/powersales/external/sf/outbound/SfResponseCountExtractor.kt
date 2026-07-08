package com.otoki.powersales.external.sf.outbound

import org.slf4j.LoggerFactory
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

/**
 * SF Apex REST 응답 본문(JSON)에서 데이터 레코드 건수를 산출하는 유틸.
 *
 * `external_api_log.response_count` 적재를 위해 [ExternalApiLogInterceptor] 의 count resolver 로 주입된다.
 * SF 응답에서 데이터 배열을 찾는 규칙은 fetch 계열 클라이언트
 * ([com.otoki.powersales.domain.sales.sfsync.SalesProgressRateMasterFetchClientImpl] 등) 의
 * `extractArrayNode` 와 동일하게 맞춘다 — 최상위가 배열이면 그대로, 객체면 흔한 wrapper key 를 탐색.
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
            val arrayNode = extractArrayNode(objectMapper.readTree(responseBody)) ?: return null
            arrayNode.size()
        } catch (ex: Exception) {
            log.debug("[sf-response-count] 응답 건수 산출 파싱 실패 — null. error={}", ex.message)
            null
        }
    }

    /** 최상위가 배열이면 그대로, 객체면 흔한 wrapper key 를 순서대로 탐색. */
    private fun extractArrayNode(root: JsonNode): JsonNode? {
        if (root.isArray) return root
        if (root.isObject) {
            for (key in WRAPPER_KEYS) {
                val child = root.get(key)
                if (child != null && child.isArray) return child
            }
        }
        return null
    }

    companion object {
        /** SF 응답 wrapper key 후보 (fetch 클라이언트 / web extractRows 와 동일 순서). */
        private val WRAPPER_KEYS = listOf("data", "DATA", "list", "LIST", "result", "RESULT", "items")
    }
}
