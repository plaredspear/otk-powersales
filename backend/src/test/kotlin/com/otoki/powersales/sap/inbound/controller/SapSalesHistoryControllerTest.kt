package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.sharing.service.FlsService
import com.otoki.powersales.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.SalesHistoryDetail
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sap.inbound.service.SapDailySalesHistoryService
import com.otoki.powersales.sap.inbound.service.SapMonthlySalesHistoryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import io.mockk.every
import io.mockk.verify
import com.ninjasquad.springmockk.MockkBean
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

    @MockkBean
    private lateinit var sapDailySalesHistoryService: SapDailySalesHistoryService

    @MockkBean
    private lateinit var sapMonthlySalesHistoryService: SapMonthlySalesHistoryService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var flsService: FlsService

    @MockkBean
    private lateinit var permissionSetEvaluator: PermissionSetEvaluator

    @MockkBean
    private lateinit var userRepository: UserRepository


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
            every { sapDailySalesHistoryService.upsert(any()) } returns                 SalesHistoryDetail(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    chunks = listOf(ChunkResult(0, ChunkResult.STATUS_SUCCESS, 1))
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

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.sap.inbound.controller.SapSalesHistoryControllerTest#invalidDailyPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun upsert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/daily-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapDailySalesHistoryService.upsert(any()) }
        }

        @Test
        @DisplayName("실패 - 행 수 한도 초과 -> 413 PAYLOAD_TOO_LARGE")
        fun upsert_payloadTooLarge() {
            every { sapDailySalesHistoryService.upsert(any()) } throws
                SapPayloadTooLargeException(50000, 50001)

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
            every { sapMonthlySalesHistoryService.upsert(any()) } returns SalesHistoryDetail(
                successCount = 1,
                failureCount = 0,
                failures = emptyList(),
                chunks = listOf(ChunkResult(0, ChunkResult.STATUS_SUCCESS, 1))
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

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.sap.inbound.controller.SapSalesHistoryControllerTest#invalidMonthlyPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun upsert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/monthly-sales-history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapMonthlySalesHistoryService.upsert(any()) }
        }
    }

    companion object {
        @JvmStatic
        fun invalidDailyPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"SAPAccountCode":"1032619","SalesDate":"20260427"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )

        @JvmStatic
        fun invalidMonthlyPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"SAPAccountCode":"1032619","SalesYearMonth":"202604"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )
    }
}
