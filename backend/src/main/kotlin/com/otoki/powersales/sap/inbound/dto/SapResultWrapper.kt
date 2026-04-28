package com.otoki.powersales.sap.inbound.dto

/**
 * SAP 인바운드 공통 응답 래퍼.
 *
 * 글로벌 [com.otoki.powersales.common.dto.ApiResponse] 컨벤션을 우회하고 레거시 Salesforce
 * `IF_REST_SAP_*` 응답 형태(`RESULT_CODE` / `RESULT_MSG`) 를 그대로 사용한다 (Spec #557, #558).
 *
 * 부분 실패 정보 등 추가 데이터가 있는 인터페이스는 [resultDetail] 필드에 페이로드를 담는다.
 * `RESULT_DETAIL` 내부 키는 SAP 측 호환과 무관한 신규 backend 정의이므로 글로벌 SNAKE_CASE
 * 정책을 따른다 (예: `success_count`).
 */
data class SapResultWrapper<T>(
    @com.fasterxml.jackson.annotation.JsonProperty("RESULT_CODE")
    val resultCode: String,
    @com.fasterxml.jackson.annotation.JsonProperty("RESULT_MSG")
    val resultMsg: String,
    @com.fasterxml.jackson.annotation.JsonProperty("RESULT_DETAIL")
    val resultDetail: T? = null
) {
    companion object {
        const val CODE_OK: String = "200"
        const val CODE_INVALID_PAYLOAD: String = "INVALID_PAYLOAD"
        const val CODE_INTERNAL_ERROR: String = "INTERNAL_ERROR"

        fun <T> ok(detail: T? = null): SapResultWrapper<T> = SapResultWrapper(CODE_OK, "OK", detail)
    }
}
