package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.FailureItem
import com.otoki.powersales.sap.inbound.service.SapAccountCategoryService
import com.otoki.powersales.sap.inbound.service.SapClientMasterService
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

@WebMvcTest(SapAccountMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapAccountMasterController 테스트")
class SapAccountMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapClientMasterService: SapClientMasterService

    @MockitoBean
    private lateinit var sapAccountCategoryService: SapAccountCategoryService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.account.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/account")
    inner class UpsertAccount {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, 부분 실패 0")
        fun upsert_success() {
            whenever(sapClientMasterService.upsert(any())).thenReturn(
                AccountMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "req_item_list": [
                    { "SAPAccountCode": "1032619", "Name": "(주)홍길동상회", "EmployeeCode": "100123" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/account")
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
            whenever(sapClientMasterService.upsert(any())).thenReturn(
                AccountMasterDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(FailureItem("1032620", "Name 필수"))
                )
            )

            val payload = """
                {
                  "req_item_list": [
                    { "SAPAccountCode": "1032619", "Name": "(주)홍길동상회" },
                    { "SAPAccountCode": "1032620" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].identifier").value("1032620"))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].reason").value("Name 필수"))
        }

        @Test
        @DisplayName("실패 - req_item_list 누락 -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapClientMasterService, never()).upsert(any())
        }

        @Test
        @DisplayName("실패 - req_item_list 빈 배열 -> INVALID_PAYLOAD")
        fun upsert_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"req_item_list": []}""")
            )
                .andExpect(status().`is`(422))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapClientMasterService, never()).upsert(any())
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sap/account-category")
    inner class UpsertAccountCategory {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200")
        fun upsert_success() {
            whenever(sapAccountCategoryService.upsert(any())).thenReturn(
                AccountMasterDetail(successCount = 2, failureCount = 0, failures = emptyList())
            )

            val payload = """
                {
                  "req_item_list": [
                    { "AccountCode": "Z001", "Name": "일반거래처" },
                    { "AccountCode": "Z002", "Name": "위탁거래처" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/account-category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(2))
        }

        @Test
        @DisplayName("실패 - req_item_list null -> INVALID_PAYLOAD")
        fun upsert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/account-category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapAccountCategoryService, never()).upsert(any())
        }
    }
}
