package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * RDP 인바운드 공통 응답 래퍼.
 *
 * SAP/SF 측 ResultWrapper 와 동일한 형태(`RESULT_CODE` / `RESULT_MSG` / `RESULT_DETAIL`) 를 사용해
 * 외부 연동 클라이언트가 일관된 응답 형식으로 처리 가능하도록 호환성을 유지한다.
 * 단 audit / scope / token 시스템은 SAP/SF 와 완전 분리.
 */
data class RdpResultWrapper<T>(
    @JsonProperty("RESULT_CODE")
    val resultCode: String,
    @JsonProperty("RESULT_MSG")
    val resultMsg: String,
    @JsonProperty("RESULT_DETAIL")
    val resultDetail: T? = null
) {
    companion object {
        const val CODE_OK: String = "200"
        const val CODE_INVALID_PARAMETER: String = "INVALID_PARAMETER"
        const val CODE_INSUFFICIENT_SCOPE: String = "INSUFFICIENT_SCOPE"
        const val CODE_INTERNAL_ERROR: String = "INTERNAL_ERROR"

        fun <T> ok(detail: T? = null): RdpResultWrapper<T> = RdpResultWrapper(CODE_OK, "OK", detail)
    }
}
