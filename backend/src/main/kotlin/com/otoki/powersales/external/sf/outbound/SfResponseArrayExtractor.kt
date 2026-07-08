package com.otoki.powersales.external.sf.outbound

import tools.jackson.databind.JsonNode

/**
 * SF Apex REST 응답 본문(JSON)에서 레코드 배열을 추출하는 공용 규칙.
 *
 * 최상위가 배열이면 그대로, 객체면 흔한 wrapper key 를 순서대로 탐색한다.
 * wrapper key 비교는 **대소문자를 무시** — 운영 SF 응답은 최상위 key 가 `"Result"`
 * (대문자 R + 소문자 esult) 로 오므로, `Result`/`result`/`RESULT` 등 철자 변형을 모두 방어한다.
 *
 * fetch/sync 계열 클라이언트(거래처목표 / 사원평가 / 클레임·물류클레임 마스터 sync)와
 * [SfResponseCountExtractor] 가 동일 규칙을 공유하도록 이 유틸 한 곳으로 모은다 —
 * wrapper key 후보나 대소문자 처리를 각 파일에 복붙하지 않는다.
 */
object SfResponseArrayExtractor {

    /** SF 응답 wrapper key 후보 (대소문자 무시 매칭). */
    private val WRAPPER_KEYS = listOf("result", "data", "list", "items")

    /** 최상위가 배열이면 그대로, 객체면 wrapper key 를 대소문자 무시로 탐색. 배열을 못 찾으면 null. */
    fun extractArrayNode(root: JsonNode): JsonNode? {
        if (root.isArray) return root
        if (root.isObject) {
            val fieldNames = root.propertyNames().asSequence().toList()
            for (candidate in WRAPPER_KEYS) {
                val matchedKey = fieldNames.firstOrNull { it.equals(candidate, ignoreCase = true) } ?: continue
                val child = root.get(matchedKey)
                if (child != null && child.isArray) return child
            }
        }
        return null
    }
}
