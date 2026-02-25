package com.otoki.internal.common.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GeoUtils 테스트")
class GeoUtilsTest {

    @Nested
    @DisplayName("calculateDistance - 두 좌표 간 거리 계산")
    inner class CalculateDistanceTests {

        // 강남역: lat=37.4979, lon=127.0276
        // 삼성역: lat=37.5088, lon=127.0632
        // 부산역: lat=35.1152, lon=129.0423

        @Test
        @DisplayName("서울 내 근거리 - 강남역에서 삼성역까지 약 3.4km")
        fun shortDistance_gangnamToSamseong() {
            // given
            val gangnamLat = 37.4979
            val gangnamLon = 127.0276
            val samseongLat = 37.5088
            val samseongLon = 127.0632

            // when
            val distance = GeoUtils.calculateDistance(
                gangnamLat, gangnamLon, samseongLat, samseongLon
            )

            // then
            assertThat(distance).isCloseTo(3.4, Offset.offset(0.5))
        }

        @Test
        @DisplayName("동일 좌표 입력 시 거리 0.0km 반환")
        fun sameCoordinates_returnsZero() {
            // given
            val lat = 37.4979
            val lon = 127.0276

            // when
            val distance = GeoUtils.calculateDistance(lat, lon, lat, lon)

            // then
            assertThat(distance).isEqualTo(0.0)
        }

        @Test
        @DisplayName("서울에서 부산까지 장거리 약 325km")
        fun longDistance_seoulToBusan() {
            // given
            val seoulLat = 37.4979
            val seoulLon = 127.0276
            val busanLat = 35.1152
            val busanLon = 129.0423

            // when
            val distance = GeoUtils.calculateDistance(
                seoulLat, seoulLon, busanLat, busanLon
            )

            // then
            assertThat(distance).isCloseTo(325.0, Offset.offset(20.0))
        }
    }
}
