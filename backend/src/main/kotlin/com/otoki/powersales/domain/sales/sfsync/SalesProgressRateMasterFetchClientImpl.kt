package com.otoki.powersales.domain.sales.sfsync

import com.otoki.powersales.external.sf.outbound.SfOutboundClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * [SalesProgressRateMasterFetchClient] 운영 구현 — SF Apex REST `/IF_salesprogresssend` 호출.
 *
 * 처리:
 *  1. `{ "MOD_DT": modDt }` 를 [SfOutboundClient.callApi] 로 POST (클레임 등록과 동일한 OAuth/401 재시도 경로).
 *  2. 응답 rawBody(JSON)에서 거래처목표 레코드 배열을 추출 — 최상위 배열 또는 `data/list/result` 등
 *     흔한 wrapper key 를 순서대로 탐색 (SF 응답 wrapper 형식이 확정 전이므로 후보 탐색).
 *  3. 각 레코드를 [SalesProgressRateMasterSfRecord] 로 역직렬화 후 [SalesProgressRateMasterFetchDto] 로 변환.
 *
 * 호출 실패(OAuth/HTTP/파싱 예외)는 빈 리스트로 흡수하여 sync 배치를 깨뜨리지 않는다 (다음 사이클 재시도).
 */
@Component
class SalesProgressRateMasterFetchClientImpl(
    private val sfOutboundClient: SfOutboundClient,
    private val objectMapper: ObjectMapper,
) : SalesProgressRateMasterFetchClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetch(modDt: String): List<SalesProgressRateMasterFetchDto> {
        val apiMap = mapOf<String, Any?>("MOD_DT" to modDt)
        val raw = try {
            val response = sfOutboundClient.callApi(SF_ENDPOINT, apiMap)
            response.rawBody
        } catch (e: Exception) {
            log.warn("[sales-progress-sync] SF fetch 호출 실패 — 빈 리스트 반환. modDt={} error={}", modDt, e.message)
            return emptyList()
        }

        return parse(raw)
    }

    override fun parse(raw: String?): List<SalesProgressRateMasterFetchDto> =
        parseRecords(raw).map { it.toFetchDto() }

    /** rawBody JSON 에서 거래처목표 레코드 배열을 추출. 형식 불명/파싱 실패 시 빈 리스트. */
    private fun parseRecords(raw: String?): List<SalesProgressRateMasterSfRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val root = objectMapper.readTree(raw)
            val arrayNode = extractArrayNode(root) ?: run {
                log.warn("[sales-progress-sync] SF 응답에서 레코드 배열을 찾지 못함 — 빈 리스트. body 앞부분={}", raw.take(200))
                return emptyList()
            }
            objectMapper.convertValue(arrayNode, object : TypeReference<List<SalesProgressRateMasterSfRecord>>() {})
        } catch (e: Exception) {
            log.warn("[sales-progress-sync] SF 응답 파싱 실패 — 빈 리스트. error={} body 앞부분={}", e.message, raw.take(200))
            emptyList()
        }
    }

    /**
     * 최상위가 배열이면 그대로, 객체면 흔한 wrapper key 를 탐색.
     *
     * 운영 SF 응답은 최상위 key 가 `"Result"` (대문자 R + 소문자 esult) 이므로 wrapper key 비교는
     * 대소문자를 무시한다 — SF 응답 철자 변형(`Result`/`result`/`RESULT` 등) 전반을 방어.
     */
    private fun extractArrayNode(root: JsonNode): JsonNode? {
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

    companion object {
        /** SF Apex REST suffix (apex base URL 뒤에 붙는다). */
        const val SF_ENDPOINT = "/IF_salesprogresssend"

        /** SF 응답 wrapper key 후보 (대소문자 무시 매칭 — [extractArrayNode] 참고). */
        private val WRAPPER_KEYS = listOf("result", "data", "list", "items")
    }
}
