package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.platform.common.security.GpsConsentFilter
import com.otoki.powersales.platform.common.security.JwtAuthenticationFilter
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.account.FailureItem
import com.otoki.powersales.external.sap.inbound.service.SapAccountCategoryService
import com.otoki.powersales.external.sap.inbound.service.SapClientMasterService
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

@WebMvcTest(SapAccountMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapAccountMasterController 테스트")
class SapAccountMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var sapClientMasterService: SapClientMasterService

    @MockkBean
    private lateinit var sapAccountCategoryService: SapAccountCategoryService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.account.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/account")
    inner class UpsertAccount {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, 부분 실패 0")
        fun upsert_success() {
            every { sapClientMasterService.upsert(any()) } returns                 AccountMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())

            val payload = """
                {
                  "reqItemList": [
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
            every { sapClientMasterService.upsert(any()) } returns                 AccountMasterDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(FailureItem("1032620", "Name 필수"))
            )

            val payload = """
                {
                  "reqItemList": [
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

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.external.sap.inbound.controller.SapAccountMasterControllerTest#invalidAccountPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun upsert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapClientMasterService.upsert(any()) }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/sap/account-category")
    inner class UpsertAccountCategory {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200")
        fun upsert_success() {
            every { sapAccountCategoryService.upsert(any()) } returns                 AccountMasterDetail(successCount = 2, failureCount = 0, failures = emptyList())

            val payload = """
                {
                  "reqItemList": [
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
        @DisplayName("도메인 예외 - 200, RESULT_CODE 0, RESULT_MSG Failed (레거시 IF_REST_SAP_AccountMaster §4 동등)")
        fun upsert_domainException_returnsFailed() {
            every { sapAccountCategoryService.upsert(any()) } throws RuntimeException("DB 오류")

            val payload = """
                { "reqItemList": [ { "AccountCode": "Z001", "Name": "일반거래처" } ] }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/account-category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("0"))
                .andExpect(jsonPath("$.RESULT_MSG").value("Failed"))
        }

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.external.sap.inbound.controller.SapAccountMasterControllerTest#invalidAccountCategoryPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun upsert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/account-category")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapAccountCategoryService.upsert(any()) }
        }
    }

    companion object {
        @JvmStatic
        fun invalidAccountPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"SAPAccountCode":"1032619"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )

        @JvmStatic
        fun invalidAccountCategoryPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"AccountCode":"Z001"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )
    }
}
