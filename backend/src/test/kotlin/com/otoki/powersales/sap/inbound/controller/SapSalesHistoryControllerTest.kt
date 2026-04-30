package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sap.inbound.service.SapDailySalesHistoryService
import com.otoki.powersales.sap.inbound.service.SapMonthlySalesHistoryService
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

@WebMvcTest(SapSalesHistoryController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapSalesHistoryController 테스트")
class SapSalesHistoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapDailySalesHistoryService: SapDailySalesHistoryService

    @MockitoBean
    private lateinit var sapMonthlySalesHistoryService: SapMonthlySalesHistoryService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.sales.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/daily-sales-history")
    inner class UpsertDaily {

        @Test
        @DisplayName("성공 - 200, RESULT_DETAIL.success_count=1, chunks 1개")
        fun upsert_success() {
            whenever(sapDailySalesHistoryService.upsert(any())).thenReturn(
                SalesHistoryDetail(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    chunks = listOf(ChunkResult(0, ChunkResult.STATUS_SUCCESS, 1))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    { "SAPAccountCode": "1032619", "SalesDate": "20260427", "ERPSalesAmount1": "1500000" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/daily-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.chunks[0].status").value("success"))
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> 422 INVALID_PAYLOAD")
        fun upsert_emptyList() {
            mockMvc.perform(
                post("/api/v1/sap/daily-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(status().`is`(422))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapDailySalesHistoryService, never()).upsert(any())
        }

        @Test
        @DisplayName("실패 - 행 수 한도 초과 -> 413 PAYLOAD_TOO_LARGE")
        fun upsert_payloadTooLarge() {
            whenever(sapDailySalesHistoryService.upsert(any()))
                .thenThrow(SapPayloadTooLargeException(50000, 50001))

            val payload = """
                {
                  "reqItemList": [
                    { "SAPAccountCode": "1032619", "SalesDate": "20260427" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/daily-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(413))
                .andExpect(jsonPath("$.RESULT_CODE").value("PAYLOAD_TOO_LARGE"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sap/monthly-sales-history")
    inner class UpsertMonthly {

        @Test
        @DisplayName("성공 - 200")
        fun upsert_success() {
            whenever(sapMonthlySalesHistoryService.upsert(any())).thenReturn(
                SalesHistoryDetail(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    chunks = listOf(ChunkResult(0, ChunkResult.STATUS_SUCCESS, 1))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    {
                      "SAPAccountCode": "1032619",
                      "SalesYearMonth": "202604",
                      "ABCClosingAmount1": "5000000",
                      "ShipClosingAmount": "4800000",
                      "rlsales": "0"
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/monthly-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/monthly-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapMonthlySalesHistoryService, never()).upsert(any())
        }
    }
}
