package com.otoki.powersales.admin.dto.response

import com.otoki.powersales.common.naver.NaverGeocodeResponse

data class NaverGeocodeTestResponse(
    val input: String,
    val matchedCount: Int,
    val results: List<Result>
) {
    data class Result(
        val roadAddress: String?,
        val jibunAddress: String?,
        val longitude: String?,
        val latitude: String?
    )

    companion object {
        fun from(input: String, response: NaverGeocodeResponse): NaverGeocodeTestResponse {
            val results = response.addresses.map { address ->
                Result(
                    roadAddress = address.roadAddress,
                    jibunAddress = address.jibunAddress,
                    longitude = address.x,
                    latitude = address.y
                )
            }
            return NaverGeocodeTestResponse(
                input = input,
                matchedCount = results.size,
                results = results
            )
        }
    }
}
