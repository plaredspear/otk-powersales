package com.otoki.powersales.common.naver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Naver Cloud Map Geocode API 응답 DTO.
 *
 * 매핑 필드:
 * - `x` / `y` — 좌표 (longitude / latitude). 레거시 `Batch_AccountLatLong.cls` 사용 (#637).
 * - `roadAddress` / `jibunAddress` — 도로명 / 지번 주소. Spec #638 admin 변환 테스트 도구 응답에 사용.
 *
 * 그 외 응답 필드는 `@JsonIgnoreProperties(ignoreUnknown = true)` 로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverGeocodeResponse(
    val addresses: List<Address> = emptyList()
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Address(
        val x: String? = null,
        val y: String? = null,
        val roadAddress: String? = null,
        val jibunAddress: String? = null
    )
}
