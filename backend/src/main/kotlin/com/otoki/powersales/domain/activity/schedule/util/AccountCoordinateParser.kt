package com.otoki.powersales.domain.activity.schedule.util

/**
 * Account.latitude / longitude (String? VARCHAR(100)) → Double 안전 변환 유틸 (Spec #585 §6).
 *
 * Salesforce / Heroku Connect 호환을 위해 Account 엔티티는 위경도를 String 으로 보관한다.
 * 신규 코드(예: GPS 거리 검증) 는 Double 로 변환해서 사용해야 하므로,
 * 누락(null/blank/parse fail/range out) 판정과 변환을 한 곳에서 처리한다.
 *
 * 누락 판정 케이스 (모두 [Coords.Missing] 반환):
 * 1. null
 * 2. 빈 문자열 또는 공백만 포함
 * 3. Double 파싱 실패
 * 4. 위도 ±90 / 경도 ±180 범위 초과
 */
object AccountCoordinateParser {

    private const val LAT_MIN = -90.0
    private const val LAT_MAX = 90.0
    private const val LNG_MIN = -180.0
    private const val LNG_MAX = 180.0

    sealed class Coords {
        data class Valid(val latitude: Double, val longitude: Double) : Coords()
        data object Missing : Coords()
    }

    fun parse(latitude: String?, longitude: String?): Coords {
        val lat = parseLatitude(latitude) ?: return Coords.Missing
        val lng = parseLongitude(longitude) ?: return Coords.Missing
        return Coords.Valid(lat, lng)
    }

    private fun parseLatitude(value: String?): Double? = parseInRange(value, LAT_MIN, LAT_MAX)

    private fun parseLongitude(value: String?): Double? = parseInRange(value, LNG_MIN, LNG_MAX)

    private fun parseInRange(value: String?, min: Double, max: Double): Double? {
        if (value.isNullOrBlank()) return null
        val parsed = value.trim().toDoubleOrNull() ?: return null
        if (parsed < min || parsed > max) return null
        return parsed
    }
}
