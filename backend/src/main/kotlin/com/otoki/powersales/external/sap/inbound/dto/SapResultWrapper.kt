package com.otoki.powersales.external.sap.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SAP 인바운드 공통 응답 래퍼.
 *
 * 글로벌 [com.otoki.powersales.common.dto.ApiResponse] 컨벤션을 우회하고 레거시 Salesforce
 * `IF_REST_SAP_*` 응답 형태(`RESULT_CODE` / `RESULT_MSG`) 를 그대로 사용한다 (Spec #557, #558).
 *
 * 부분 실패 정보 등 추가 데이터가 있는 인터페이스는 [resultDetail] 필드에 페이로드를 담는다.
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부는 SnakeCase 유지 — 각 Detail DTO 의 `@JsonNaming`
 * 어노테이션으로 보장한다 (Spec #580 P1-B). 외부 래퍼 `RESULT_CODE` / `RESULT_MSG` /
 * `RESULT_DETAIL` 키는 [JsonProperty] 로 보존한다.
 */
data class SapResultWrapper<T>(
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
        const val CODE_METHOD_NOT_ALLOWED: String = "METHOD_NOT_ALLOWED"
        const val CODE_UNSUPPORTED_MEDIA_TYPE: String = "UNSUPPORTED_MEDIA_TYPE"
        const val CODE_NOT_ACCEPTABLE: String = "NOT_ACCEPTABLE"
        const val CODE_INTERNAL_ERROR: String = "INTERNAL_ERROR"

        fun <T> ok(detail: T? = null): SapResultWrapper<T> = SapResultWrapper(CODE_OK, "OK", detail)
    }
}
