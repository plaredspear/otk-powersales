package com.otoki.powersales.admin.dto.response

/**
 * Naver Geocode 변환 테스트 응답 (#638).
 *
 * Naver Cloud Map Geocode API 응답 본문을 가공 없이 raw JSON 문자열 그대로 노출 — 운영자가 API 원본 응답을
 * 직접 확인할 수 있도록 함.
 */
data class NaverGeocodeTestResponse(
    val input: String,
    val rawResponse: String
)
