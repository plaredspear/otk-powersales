package com.otoki.powersales.claim.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.claim.dto.sap.ClaimStatusDetail
import com.otoki.powersales.claim.dto.sap.ClaimStatusFailure
import com.otoki.powersales.claim.service.SapClaimStatusService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.controller.SapInboundExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapClaimStatusController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapClaimStatusController 테스트")
class SapClaimStatusControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapClaimStatusService: SapClaimStatusService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "otoki-sap-client",
                null,
                listOf(SimpleGrantedAuthority("SCOPE_sap.claim.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/claim-status")
    inner class UpdateClaimStatus {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count 1")
        fun update_success() {
            whenever(sapClaimStatusService.update(any())).thenReturn(
                ClaimStatusDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    {
                      "Name": "CLM-2026-04-001",
                      "ClaimSequence": "001",
                      "ActionCode": "AC02",
                      "ClaimStatus": "처리완료",
                      "Content": "교환 처리 완료"
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/claim-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(0))
        }

        @Test
        @DisplayName("부분 실패 - failures 에 name / reason 포함")
        fun update_partialFailure() {
            whenever(sapClaimStatusService.update(any())).thenReturn(
                ClaimStatusDetail(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(ClaimStatusFailure("CLM-NOTEXIST", "claim not found"))
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/claim-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList":[{"Name":"CLM-NOTEXIST"}]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].name").value("CLM-NOTEXIST"))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].reason").value("claim not found"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> INVALID_PAYLOAD")
        fun update_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/claim-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapClaimStatusService, never()).update(any())
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> INVALID_PAYLOAD")
        fun update_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/claim-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapClaimStatusService, never()).update(any())
        }
    }
}
