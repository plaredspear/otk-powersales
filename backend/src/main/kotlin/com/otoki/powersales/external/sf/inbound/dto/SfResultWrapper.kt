package com.otoki.powersales.external.sf.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SF 인바운드 공통 응답 래퍼.
 *
 * SAP 측 [com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper] 와 동일한 형태 (`RESULT_CODE` /
 * `RESULT_MSG` / `RESULT_DETAIL`) 를 사용한다 — SF Apex 측 호출자가 SAP 와 일치된 응답 형식으로
 * 처리 가능하도록 호환성 유지. 단 audit / scope / token 시스템은 완전 분리 (Spec #774 Q3, #775 Q7).
 */
data class SfResultWrapper<T>(
    @JsonProperty("RESULT_CODE")
    val resultCode: String,
    @JsonProperty("RESULT_MSG")
    val resultMsg: String,
    @JsonProperty("RESULT_DETAIL")
    val resultDetail: T? = null
) {
    companion object {
        const val CODE_OK: String = "200"
        const val CODE_INVALID_PAYLOAD: String = "INVALID_PAYLOAD"
        const val CODE_INSUFFICIENT_SCOPE: String = "INSUFFICIENT_SCOPE"
        const val CODE_PAYLOAD_TOO_LARGE: String = "PAYLOAD_TOO_LARGE"
        const val CODE_INTERNAL_ERROR: String = "INTERNAL_ERROR"

        fun <T> ok(detail: T? = null): SfResultWrapper<T> = SfResultWrapper(CODE_OK, "OK", detail)
    }
}
