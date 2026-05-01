package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.sap.inbound.dto.employee.FailureItem
import com.otoki.powersales.sap.inbound.service.SapEmployeeMasterService
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

@WebMvcTest(SapEmployeeMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapEmployeeMasterController 테스트")
class SapEmployeeMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapEmployeeMasterService: SapEmployeeMasterService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.employee.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/employee")
    inner class UpsertEmployee {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, 부분 실패 0")
        fun upsert_success() {
            whenever(sapEmployeeMasterService.upsert(any())).thenReturn(
                EmployeeMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "reqItemList": [
                    { "EmployeeCode": "100123", "EmployeeName": "홍길동" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/employee")
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
        @DisplayName("부분 실패 - 200, failures 페이로드 포함")
        fun upsert_partialFailure() {
            whenever(sapEmployeeMasterService.upsert(any())).thenReturn(
                EmployeeMasterDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(FailureItem("100124", "EmployeeName 필수"))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    { "EmployeeCode": "100123", "EmployeeName": "홍길동" },
                    { "EmployeeCode": "100124" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/employee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].emp_code").value("100124"))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].reason").value("EmployeeName 필수"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/employee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapEmployeeMasterService, never()).upsert(any())
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> INVALID_PAYLOAD")
        fun upsert_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/employee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapEmployeeMasterService, never()).upsert(any())
        }
    }
}
