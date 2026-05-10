package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.common.naver.NaverApiException
import com.otoki.powersales.common.naver.NaverGeocodeClient
import com.otoki.powersales.common.naver.NaverGeocodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminNaverGeocodeService 테스트")
class AdminNaverGeocodeServiceTest {

    @Mock private lateinit var naverGeocodeClient: NaverGeocodeClient
    @InjectMocks private lateinit var service: AdminNaverGeocodeService

    @Nested
    @DisplayName("test - Naver Geocode 변환")
    inner class TestGeocode {

        @Test
        @DisplayName("H1 성공 - 정상 주소 1건 매칭 -> 응답 매핑")
        fun test_happyPath() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            val response = NaverGeocodeResponse(
                addresses = listOf(
                    NaverGeocodeResponse.Address(
                        x = "127.0584",
                        y = "37.5074",
                        roadAddress = "서울특별시 강남구 테헤란로 123",
                        jibunAddress = "서울특별시 강남구 역삼동 123-45"
                    )
                )
            )
            whenever(naverGeocodeClient.geocode(address)).thenReturn(response)

            val result = service.test(userId = 1L, request = request)

            assertThat(result.input).isEqualTo(address)
            assertThat(result.matchedCount).isEqualTo(1)
            assertThat(result.results).hasSize(1)
            assertThat(result.results[0].roadAddress).isEqualTo("서울특별시 강남구 테헤란로 123")
            assertThat(result.results[0].jibunAddress).isEqualTo("서울특별시 강남구 역삼동 123-45")
            assertThat(result.results[0].longitude).isEqualTo("127.0584")
            assertThat(result.results[0].latitude).isEqualTo("37.5074")
        }

        @Test
        @DisplayName("H2 매칭 0건 - 빈 addresses -> matchedCount=0 + 빈 results")
        fun test_zeroMatch() {
            val address = "잘못된 주소"
            val request = NaverGeocodeTestRequest(address = address)
            whenever(naverGeocodeClient.geocode(address)).thenReturn(NaverGeocodeResponse(addresses = emptyList()))

            val result = service.test(userId = 1L, request = request)

            assertThat(result.input).isEqualTo(address)
            assertThat(result.matchedCount).isEqualTo(0)
            assertThat(result.results).isEmpty()
        }

        @Test
        @DisplayName("E2 Naver API 5xx - client 가 null 반환 -> NaverApiException throw")
        fun test_naverApiFailure() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            whenever(naverGeocodeClient.geocode(address)).thenReturn(null)

            assertThatThrownBy { service.test(userId = 1L, request = request) }
                .isInstanceOf(NaverApiException::class.java)
                .hasMessageContaining("Naver Geocode API 호출 실패")
        }

        @Test
        @DisplayName("E6 Naver API 타임아웃 - client 가 null 반환 (5xx 와 통합) -> NaverApiException throw")
        fun test_naverApiTimeout() {
            val address = "서울특별시 강남구 테헤란로 123"
            val request = NaverGeocodeTestRequest(address = address)
            whenever(naverGeocodeClient.geocode(address)).thenReturn(null)

            assertThatThrownBy { service.test(userId = 1L, request = request) }
                .isInstanceOf(NaverApiException::class.java)
        }
    }
}
