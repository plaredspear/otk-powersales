package com.otoki.powersales.external.sap.outbound.guard

import tools.jackson.databind.ObjectMapper

/**
 * SAP REST 응답 본문의 `resultCode` / `resutlMsg` 파싱 규칙 SoT.
 *
 * SAP 정상 응답은 `{"resultCode":"S", ...}` 형태이며, 메시지 필드는 SAP 측 오타로 `resutlMsg` 가
 * 1순위, 표준 철자 `resultMsg` 가 fallback 이다. 이 규칙은 [com.otoki.powersales.external.sap.outbox.SapOutboxBatchService]
 * (범용 outbox 송신) 과 [com.otoki.powersales.external.sap.outbound.SapOutboundResponseSink]
 * (모든 SAP outbound 호출 이력 적재) 가 공유한다 — 한쪽만 고쳐져 해석이 어긋나는 것을 막는다.
 */
object SapResponseInterpreter {

    /** SAP 응답 성공 코드. */
    const val RESULT_CODE_SUCCESS_RAW = "S"

    /**
     * 응답 본문을 파싱해 (성공 여부, 메시지) 로 해석한다.
     *
     * - 본문 null/blank → `false` + `"EMPTY_RESPONSE"`
     * - JSON 파싱 실패 → `false` + `"INVALID_JSON: <본문 앞 200자>"`
     * - `resultCode == "S"` → `true`, 그 외 → `false`. 메시지는 `resutlMsg` ?: `resultMsg`.
     */
    fun interpret(objectMapper: ObjectMapper, body: String?): Result {
        if (body.isNullOrBlank()) return Result(success = false, message = "EMPTY_RESPONSE", resultCodeRaw = null)
        return try {
            val node = objectMapper.readTree(body)
            val rawCode = node["resultCode"]?.asString()
            val msg = node["resutlMsg"]?.asString() ?: node["resultMsg"]?.asString()
            Result(success = rawCode == RESULT_CODE_SUCCESS_RAW, message = msg, resultCodeRaw = rawCode)
        } catch (_: Exception) {
            Result(success = false, message = "INVALID_JSON: ${body.take(200)}", resultCodeRaw = null)
        }
    }

    /**
     * @param success     `resultCode == "S"` 여부
     * @param message     `resutlMsg`/`resultMsg` 또는 파싱 실패 사유
     * @param resultCodeRaw SAP 원본 `resultCode` (파싱 불가/누락 시 null)
     */
    data class Result(val success: Boolean, val message: String?, val resultCodeRaw: String?)
}
