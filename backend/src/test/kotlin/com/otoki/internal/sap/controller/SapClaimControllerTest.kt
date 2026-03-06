package com.otoki.internal.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import com.otoki.internal.sap.dto.SapClaimRequest
import com.otoki.internal.sap.dto.SapClaimResponse
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.sap.filter.SapApiKeyFilter
import com.otoki.internal.sap.service.SapClaimService
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

@WebMvcTest(SapClaimController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapClaimController 테스트")
class SapClaimControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapClaimService: SapClaimService

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
    @DisplayName("POST /api/v1/sap/claim")
    inner class SyncClaim {

        @Test
        @DisplayName("성공 - 클레임 동기화 결과 반환")
        fun `should return success when syncClaim succeeds`() {
            // given
            val request = SapClaimRequest(
                request = SapClaimRequest.ClaimItem(
                    name = "CLM-001",
                    claimSequence = "001",
                    actionCode = "A",
                    claimStatus = "OPEN",
                    content = "불량 신고",
                    reasonType = "DEFECT",
                    cosmosKey = "COS-001"
                )
            )
            whenever(sapClaimService.syncClaim(any()))
                .thenReturn(SapClaimResponse("S", "성공"))

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/claim")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("S"))
                .andExpect(jsonPath("$.result_msg").value("성공"))
        }

        @Test
        @DisplayName("클레임 미존재 - error 응답 반환")
        fun `should return error when claim not found`() {
            // given
            val request = SapClaimRequest(
                request = SapClaimRequest.ClaimItem(
                    name = "CLM-001",
                    claimSequence = "001"
                )
            )
            whenever(sapClaimService.syncClaim(any()))
                .thenReturn(SapClaimResponse("E", "해당 클레임을 찾을 수 없습니다: CLM-001"))

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/claim")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("E"))
        }

        @Test
        @DisplayName("request null - error 응답 반환")
        fun `should return error when request is null`() {
            // given
            val request = SapClaimRequest(request = null)

            // when & then
            mockMvc.perform(
                post("/api/v1/sap/claim")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("E"))
        }
    }
}
