package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderFailure
import com.otoki.powersales.sap.inbound.service.SapErpOrderService
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

@WebMvcTest(SapErpOrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapErpOrderController 테스트")
class SapErpOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapErpOrderService: SapErpOrderService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.order.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/erp-order")
    inner class UpsertErpOrder {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count 1")
        fun upsert_success() {
            whenever(sapErpOrderService.upsert(any())).thenReturn(
                ErpOrderDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    {
                      "SAPOrderNumber": "0010012345",
                      "SAPAccountCode": "1032619",
                      "ItemDetailList": [
                        { "SAPOrderNumber": "0010012345", "LineNumber": "001", "ProductCode": "100100" }
                      ]
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/erp-order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_MSG").value("OK"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(0))
        }

        @Test
        @DisplayName("부분 실패 - 200, failures 페이로드에 sap_order_number / reason 포함")
        fun upsert_partialFailure() {
            whenever(sapErpOrderService.upsert(any())).thenReturn(
                ErpOrderDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(ErpOrderFailure("0010000002", "account not found"))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    { "SAPOrderNumber": "0010000001", "SAPAccountCode": "1032619" },
                    { "SAPOrderNumber": "0010000002", "SAPAccountCode": "9999999" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/erp-order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].sap_order_number").value("0010000002"))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].reason").value("account not found"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/erp-order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapErpOrderService, never()).upsert(any())
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> INVALID_PAYLOAD")
        fun upsert_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/erp-order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapErpOrderService, never()).upsert(any())
        }
    }
}
