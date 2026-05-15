package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.admin.dto.response.NaverGeocodeTestResponse
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.admin.service.AdminNaverGeocodeService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.naver.NaverApiException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AdminNaverGeocodeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminNaverGeocodeController 테스트")
class AdminNaverGeocodeControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockitoBean private lateinit var adminNaverGeocodeService: AdminNaverGeocodeService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter
    @MockitoBean private lateinit var dataScopeHolder: DataScopeHolder

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeNumber = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("POST /api/v1/admin/naver-geocode/test - 변환 테스트")
    inner class TestGeocode {

        @Test
        @DisplayName("H1 성공 - 정상 주소 변환 -> 200 + 응답 매핑")
        fun test_success() {
            val request = NaverGeocodeTestRequest(address = "서울특별시 강남구 테헤란로 123")
            val response = NaverGeocodeTestResponse(
                input = request.address,
                matchedCount = 1,
                results = listOf(
                    NaverGeocodeTestResponse.Result(
                        roadAddress = "서울특별시 강남구 테헤란로 123",
                        jibunAddress = "서울특별시 강남구 역삼동 123-45",
                        longitude = "127.0584",
                        latitude = "37.5074"
                    )
                )
            )
            whenever(adminNaverGeocodeService.test(eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.input").value(request.address))
                .andExpect(jsonPath("$.data.matchedCount").value(1))
                .andExpect(jsonPath("$.data.results[0].roadAddress").value("서울특별시 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.data.results[0].longitude").value("127.0584"))
                .andExpect(jsonPath("$.data.results[0].latitude").value("37.5074"))
        }

        @Test
        @DisplayName("E1 실패 - address 누락(빈 문자열) -> 400 validation")
        fun test_emptyAddress() {
            val invalidJson = """{"address": ""}"""
            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("E5 실패 - address 길이 201자 -> 400 validation")
        fun test_addressTooLong() {
            val longAddress = "가".repeat(201)
            val invalidJson = """{"address": "$longAddress"}"""
            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("E2 실패 - Naver API 5xx (service 가 NaverApiException throw) -> 502 + NAVER_GEOCODE_API_FAILED")
        fun test_naverApiFailure() {
            val request = NaverGeocodeTestRequest(address = "서울특별시 강남구 테헤란로 123")
            whenever(adminNaverGeocodeService.test(eq(1L), any())).thenThrow(NaverApiException())

            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadGateway)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NAVER_GEOCODE_API_FAILED"))
        }

        @Test
        @DisplayName("H2 매칭 0건 - 응답 빈 배열 -> 200 + matchedCount=0")
        fun test_zeroMatch() {
            val request = NaverGeocodeTestRequest(address = "잘못된 주소")
            val response = NaverGeocodeTestResponse(
                input = request.address,
                matchedCount = 0,
                results = emptyList()
            )
            whenever(adminNaverGeocodeService.test(eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchedCount").value(0))
                .andExpect(jsonPath("$.data.results").isEmpty)
        }
    }
}
