package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.common.naver.NaverApiException
import com.otoki.powersales.common.naver.NaverGeocodeClient
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdminNaverGeocodeService 테스트")
class AdminNaverGeocodeServiceTest {

    private val naverGeocodeClient: NaverGeocodeClient = mockk()
    private val service = AdminNaverGeocodeService(naverGeocodeClient)

    @Nested
    @DisplayName("test - Naver Geocode 변환")
    inner class TestGeocode {

        @Test
        @DisplayName("H1 성공 - 정상 주소 1건 매칭 -> rawResponse 그대로 전달")
        fun test_happyPath() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            val rawJson = """{"status":"OK","meta":{"totalCount":1,"page":1,"count":1},"addresses":[{"roadAddress":"서울특별시 강남구 테헤란로 123","jibunAddress":"서울특별시 강남구 역삼동 123-45","x":"127.0584","y":"37.5074"}],"errorMessage":""}"""
            every { naverGeocodeClient.geocodeRaw(address) } returns rawJson

            val result = service.test(userId = 1L, request = request)

            assertThat(result.input).isEqualTo(address)
            assertThat(result.rawResponse).isEqualTo(rawJson)
        }

        @Test
        @DisplayName("H2 매칭 0건 - addresses 빈 배열 raw JSON 도 그대로 전달")
        fun test_zeroMatch() {
            val address = "잘못된 주소"
            val request = NaverGeocodeTestRequest(address = address)
            val rawJson = """{"status":"OK","meta":{"totalCount":0,"page":1,"count":0},"addresses":[],"errorMessage":""}"""
            every { naverGeocodeClient.geocodeRaw(address) } returns rawJson

            val result = service.test(userId = 1L, request = request)

            assertThat(result.input).isEqualTo(address)
            assertThat(result.rawResponse).isEqualTo(rawJson)
        }

        @Test
        @DisplayName("E2 Naver API 5xx - client 가 null 반환 -> NaverApiException throw")
        fun test_naverApiFailure() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            every { naverGeocodeClient.geocodeRaw(address) } returns null

            assertThatThrownBy { service.test(userId = 1L, request = request) }
                .isInstanceOf(NaverApiException::class.java)
                .hasMessageContaining("Naver Geocode API 호출 실패")
        }

        @Test
        @DisplayName("E6 Naver API 타임아웃 - client 가 null 반환 (5xx 와 통합) -> NaverApiException throw")
        fun test_naverApiTimeout() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            every { naverGeocodeClient.geocodeRaw(address) } returns null

            assertThatThrownBy { service.test(userId = 1L, request = request) }
                .isInstanceOf(NaverApiException::class.java)
        }
    }
}
