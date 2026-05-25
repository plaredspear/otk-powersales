package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.admin.dto.response.NaverGeocodeTestResponse
import com.otoki.powersales.admin.service.AdminNaverGeocodeService
import com.otoki.powersales.common.naver.NaverApiException
import com.otoki.powersales.common.test.AdminControllerTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(AdminNaverGeocodeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminNaverGeocodeController 테스트")
class AdminNaverGeocodeControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminNaverGeocodeService: AdminNaverGeocodeService

    @Nested
    @DisplayName("POST /api/v1/admin/naver-geocode/test - 변환 테스트")
    inner class TestGeocode {

        @Test
        @DisplayName("H1 성공 - 정상 주소 변환 -> 200 + raw JSON 응답 그대로")
        fun test_success() {
            val request = NaverGeocodeTestRequest(address = "서울특별시 강남구 테헤란로 123")
            val rawJson = """{"status":"OK","meta":{"totalCount":1,"page":1,"count":1},"addresses":[{"roadAddress":"서울특별시 강남구 테헤란로 123","jibunAddress":"서울특별시 강남구 역삼동 123-45","x":"127.0584","y":"37.5074"}],"errorMessage":""}"""
            val response = NaverGeocodeTestResponse(
                input = request.address,
                rawResponse = rawJson
            )
            every { adminNaverGeocodeService.test(eq(1L), any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.input").value(request.address))
                .andExpect(jsonPath("$.data.rawResponse").value(rawJson))
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
            every { adminNaverGeocodeService.test(eq(1L), any()) } throws NaverApiException()

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
        @DisplayName("H2 매칭 0건 - 응답 빈 배열도 raw JSON 그대로 -> 200")
        fun test_zeroMatch() {
            val request = NaverGeocodeTestRequest(address = "잘못된 주소")
            val rawJson = """{"status":"OK","meta":{"totalCount":0,"page":1,"count":0},"addresses":[],"errorMessage":""}"""
            val response = NaverGeocodeTestResponse(
                input = request.address,
                rawResponse = rawJson
            )
            every { adminNaverGeocodeService.test(eq(1L), any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/naver-geocode/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rawResponse").value(rawJson))
        }
    }
}
