package com.otoki.powersales.common.naver

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Naver Cloud Map Geocode API 응답 DTO.
 *
 * 레거시 (`Batch_AccountLatLong.cls`) 가 사용한 필드는 `addresses[0].x` (longitude),
 * `addresses[0].y` (latitude) 두 개. 본 프로젝트는 본 두 필드만 매핑하고 나머지 응답 필드는
 * `@JsonIgnoreProperties(ignoreUnknown = true)` 로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverGeocodeResponse(
    val addresses: List<Address> = emptyList()
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Address(
        val x: String? = null,
        val y: String? = null
    )
}
