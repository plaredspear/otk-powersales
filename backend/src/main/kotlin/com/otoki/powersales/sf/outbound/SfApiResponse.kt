package com.otoki.powersales.sf.outbound

/**
 * SF Apex REST endpoint 응답 — `IF_REST_MOBILE_*.cls` 의 `ResultWrapper` 정합.
 *
 * 모든 SF Apex REST endpoint 는 `{ RESULT_CODE, RESULT_MSG }` 단일 형식으로 응답한다.
 * `rawBody` 는 디버깅용 — 향후 endpoint 별 추가 필드 회수 필요 시 직접 parse.
 */
data class SfApiResponse(
    val resultCode: String,
    val resultMsg: String,
    val rawBody: String,
) {
    /** SF 의 성공 코드 `'200'` (다른 SAP/SF 인터페이스의 `'S'` 와 다름). */
    fun isSuccess(): Boolean = resultCode == "200"
}
