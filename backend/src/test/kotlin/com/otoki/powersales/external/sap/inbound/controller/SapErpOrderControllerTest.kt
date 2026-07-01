package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.platform.common.security.GpsConsentFilter
import com.otoki.powersales.platform.common.security.JwtAuthenticationFilter
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderFailure
import com.otoki.powersales.external.sap.inbound.service.SapErpOrderService
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

@WebMvcTest(SapErpOrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapErpOrderController 테스트")
class SapErpOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var sapErpOrderService: SapErpOrderService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.order.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/erp-order")
    inner class UpsertErpOrder {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count 1")
        fun upsert_success() {
            every { sapErpOrderService.upsert(any()) } returns                 ErpOrderDetail(successCount = 1, failureCount = 0, failures = emptyList())

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
            every { sapErpOrderService.upsert(any()) } returns                 ErpOrderDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(ErpOrderFailure("0010000002", "account not found"))
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

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.external.sap.inbound.controller.SapErpOrderControllerTest#invalidPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun upsert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/erp-order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapErpOrderService.upsert(any()) }
        }

        @ParameterizedTest(name = "reqItemList 키 대소문자 변형: {0} → 200, upsert 호출")
        @MethodSource("com.otoki.powersales.external.sap.inbound.controller.SapErpOrderControllerTest#reqItemListAliasCases")
        @DisplayName("성공 - 레거시 deserializeStrict 정합: reqItemList 대소문자 변형을 @JsonAlias 로 흡수")
        fun upsert_reqItemListCaseAlias(topLevelKey: String) {
            every { sapErpOrderService.upsert(any()) } returns
                ErpOrderDetail(successCount = 1, failureCount = 0, failures = emptyList())

            val payload = """
                {
                  "$topLevelKey": [
                    { "SAPOrderNumber": "0010012345", "SAPAccountCode": "1032619" }
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

            verify(exactly = 1) { sapErpOrderService.upsert(any()) }
        }

        @Test
        @DisplayName("성공 - SAP 실제 페이로드: 숫자 타입 필드(EmployeeCode/OrderSalesAmount/ProductCode/OrderQuantity)를 String 필드로 흡수")
        fun upsert_numericFieldsCoercedToString() {
            every { sapErpOrderService.upsert(any()) } returns
                ErpOrderDetail(successCount = 1, failureCount = 0, failures = emptyList())

            // SAP RESTAdapter 는 코드/수량/금액을 JSON number 로 전송한다 (DTO 는 String?).
            // 레거시 SF 는 모든 필드가 String 이라 관대했으므로, 신규도 number→String 강제 변환을 허용해야 한다.
            val payload = """
                {
                  "reqItemList": [
                    {
                      "SAPOrderNumber": "332641782",
                      "SAPAccountCode": "1051980",
                      "EmployeeCode": 20180030,
                      "OrderSalesAmount": 105681,
                      "ItemDetailList": [
                        { "SAPOrderNumber": "332641782", "LineNumber": "000010", "ProductCode": 24010246, "OrderQuantity": 4 }
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

            verify(exactly = 1) { sapErpOrderService.upsert(any()) }
        }
    }

    companion object {
        @JvmStatic
        fun invalidPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"SAPOrderNumber":"1000123456"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )

        // 레거시 SF JSON.deserializeStrict 는 키 대소문자를 구분하지 않아 SAP 가 아래 변형으로 보내도
        // 매핑에 성공했다. @JsonAlias 로 동등 흡수됨을 검증한다 (canonical 은 reqItemList).
        @JvmStatic
        fun reqItemListAliasCases(): List<String> = listOf(
            "reqItemList",   // canonical
            "ReqItemList",   // PascalCase
            "reqitemlist",   // 전부 소문자
            "REQITEMLIST",   // 전부 대문자
            "REQ_ITEM_LIST"  // snake + 대문자
        )
    }
}
