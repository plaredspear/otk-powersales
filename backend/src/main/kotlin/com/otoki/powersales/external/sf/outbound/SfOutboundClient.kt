package com.otoki.powersales.external.sf.outbound

/**
 * SF Apex REST endpoint 호출 client — Spec #829.
 *
 * Heroku `ApiService.callApi` 패턴 재현:
 *   1. `SfOAuthTokenManager.getAccessToken()` 으로 Bearer 헤더 부착
 *   2. apiMap → JSON POST → SF Apex endpoint 호출
 *   3. 응답이 401 이면 토큰 invalidate + 1회 재발급 + 재시도
 *   4. 응답 body 를 `SfApiResponse` 로 역직렬화
 *
 * endpoint 는 `sf.outbound.apex-base-url` prefix 에 suffix (예: `/ClaimRegist`) 를 붙여 호출.
 */
interface SfOutboundClient {

    /**
     * SF Apex endpoint 호출.
     *
     * @param endpoint apex base URL 뒤의 suffix (예: `"/ClaimRegist"`). 슬래시 prefix 포함.
     * @param apiMap   JSON body 로 직렬화될 입력 페이로드.
     * @return SF 응답 (`{ RESULT_CODE, RESULT_MSG }`).
     * @throws SfOAuthFailedException 토큰 발급 실패 시 (401 재발급 후에도 실패).
     */
    fun callApi(endpoint: String, apiMap: Map<String, Any?>): SfApiResponse
}
