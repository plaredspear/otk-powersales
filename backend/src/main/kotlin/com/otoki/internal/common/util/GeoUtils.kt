package com.otoki.internal.common.util

import kotlin.math.*

/**
 * GPS 거리 계산 유틸리티
 * Haversine 공식 기반 두 GPS 좌표 간 거리(km) 계산
 */
object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산
     * @return km 단위 거리 (소수점 3자리까지)
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
                cos(radLat1) * cos(radLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_KM * c * 1000).roundToLong() / 1000.0
    }
}
