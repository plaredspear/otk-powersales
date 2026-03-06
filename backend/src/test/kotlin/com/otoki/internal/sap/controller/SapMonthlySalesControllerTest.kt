package com.otoki.internal.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import com.otoki.internal.sap.dto.SapMonthlySalesRequest
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.sap.filter.SapApiKeyFilter
import com.otoki.internal.sap.service.SapMonthlySalesService
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

@WebMvcTest(SapMonthlySalesController::class)
@AutoConfigureMockMvc(addFilters = false)
class SapMonthlySalesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapMonthlySalesService: SapMonthlySalesService

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
    @DisplayName("POST /api/v1/sap/monthly-sales")
    inner class SyncMonthlySales {

        @Test
        @DisplayName("성공 - 동기화 결과 반환")
        fun `should return success when sync completes`() {
            // given
            val request = SapMonthlySalesRequest(
                reqItemList = listOf(SapMonthlySalesRequest.ReqItem(sapAccountCode = "0001234567", salesYearMonth = "202603"))
            )
            whenever(sapMonthlySalesService.sync(any()))
                .thenReturn(SapSyncResult(successCount = 1, failCount = 0))

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/monthly-sales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("SUCCESS"))
        }

        @Test
        @DisplayName("빈 요청 - EMPTY_REQUEST 반환")
        fun `should return empty response when reqItemList is empty`() {
            // given
            val request = SapMonthlySalesRequest(reqItemList = emptyList())

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/monthly-sales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("EMPTY_REQUEST"))
        }

        @Test
        @DisplayName("트랜잭션 실패 - 에러 응답 반환")
        fun `should return error response when sync throws exception`() {
            // given
            val request = SapMonthlySalesRequest(
                reqItemList = listOf(SapMonthlySalesRequest.ReqItem(sapAccountCode = "0001234567", salesYearMonth = "202603"))
            )
            whenever(sapMonthlySalesService.sync(any()))
                .thenThrow(RuntimeException("DB error"))

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/monthly-sales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
        }
    }
}
