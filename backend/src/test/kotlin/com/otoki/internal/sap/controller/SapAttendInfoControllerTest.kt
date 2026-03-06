package com.otoki.internal.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import com.otoki.internal.sap.dto.SapAttendInfoRequest
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.sap.filter.SapApiKeyFilter
import com.otoki.internal.sap.service.SapAttendInfoService
import com.otoki.internal.sap.service.SapSyncLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapAttendInfoController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapAttendInfoController 테스트")
class SapAttendInfoControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapAttendInfoService: SapAttendInfoService

    @MockitoBean
    private lateinit var sapSyncLogService: SapSyncLogService

    @MockitoBean
    private lateinit var sapAuthProperties: SapAuthProperties

    @MockitoBean
    private lateinit var sapApiKeyFilter: SapApiKeyFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Nested
    @DisplayName("POST /api/v1/sap/attend-info")
    inner class SyncAttendInfo {

        @Test
        @DisplayName("성공 - 동기화 결과 반환")
        fun `should return SUCCESS when sync succeeds`() {
            val request = SapAttendInfoRequest(
                reqItemList = listOf(SapAttendInfoRequest.ReqItem(employeeCode = "100234", startDate = "20260301"))
            )
            whenever(sapAttendInfoService.sync(any()))
                .thenReturn(SapSyncResult(successCount = 1, failCount = 0))

            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("SUCCESS"))
        }

        @Test
        @DisplayName("빈 요청 - EMPTY_REQUEST 반환")
        fun `should return EMPTY_REQUEST when reqItemList is empty`() {
            val request = SapAttendInfoRequest(reqItemList = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("EMPTY_REQUEST"))
        }

        @Test
        @DisplayName("트랜잭션 실패 - error 응답 반환")
        fun `should return error when sync throws exception`() {
            val request = SapAttendInfoRequest(
                reqItemList = listOf(SapAttendInfoRequest.ReqItem(employeeCode = "100234", startDate = "20260301"))
            )
            whenever(sapAttendInfoService.sync(any()))
                .thenThrow(RuntimeException("DB connection failed"))

            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
        }
    }
}
