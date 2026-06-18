package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.platform.common.security.GpsConsentFilter
import com.otoki.powersales.platform.common.security.JwtAuthenticationFilter
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentDetail
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.external.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.external.sap.inbound.service.SapAppointmentService
import org.assertj.core.api.Assertions.assertThat
import io.mockk.slot
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

@WebMvcTest(SapAppointmentController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapAppointmentController 테스트")
class SapAppointmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var sapAppointmentService: SapAppointmentService

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
                listOf(SimpleGrantedAuthority("SCOPE_sap.attendance.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/appointment")
    inner class InsertAppointment {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count 1")
        fun insert_success() {
            val captured = slot<List<AppointmentRequestItem>>()
            every { sapAppointmentService.insert(capture(captured)) } returns                 AppointmentDetail(successCount = 1, failureCount = 0, failures = emptyList())

            // 레거시 SAP 송신 키는 `Employeecode`(소문자 c). 이 키가 DTO `employeeCode` 로 바인딩되는지 검증
            // (대문자 `EmployeeCode` 로 두면 Jackson case-sensitive 바인딩 실패 → null → 전 행 실패).
            val payload = """
                {
                  "reqItemList": [
                    {
                      "Employeecode": "100123",
                      "AfterOrgCode": "11110",
                      "AfterOrgName": "서울지점",
                      "JobCode": "J001",
                      "JobName": "영업담당",
                      "AppointDate": "20260401",
                      "WorkType": "정규직"
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/appointment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_MSG").value("OK"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(0))

            assertThat(captured.captured.single().employeeCode).isEqualTo("100123")
        }

        @Test
        @DisplayName("부분 실패 - 200, failures 페이로드 포함")
        fun insert_partialFailure() {
            every { sapAppointmentService.insert(any()) } returns                 AppointmentDetail(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(FailureItem("100124", "JobCode 필수"))
            )

            val payload = """
                {
                  "reqItemList": [
                    { "Employeecode": "100123", "JobCode": "J001", "AppointDate": "20260401" },
                    { "Employeecode": "100124", "AppointDate": "20260401" }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/appointment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].identifier").value("100124"))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures[0].reason").value("JobCode 필수"))
        }

        @ParameterizedTest(name = "{0} → status={1}, RESULT_CODE=INVALID_PAYLOAD")
        @MethodSource("com.otoki.powersales.external.sap.inbound.controller.SapAppointmentControllerTest#invalidPayloadCases")
        @DisplayName("실패 - INVALID_PAYLOAD 변형들")
        fun insert_invalidPayload(case: String, expectedStatus: Int, payload: String) {
            mockMvc.perform(
                post("/api/v1/sap/appointment")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapAppointmentService.insert(any()) }
        }
    }

    companion object {
        @JvmStatic
        fun invalidPayloadCases(): List<Arguments> = listOf(
            Arguments.of("빈 객체", 400, """{}"""),
            Arguments.of("외부 래퍼 키 오타 (snake_case 잘못 사용)", 400, """{"req_item_list": [{"EmpCode":"E0001234"}]}"""),
            Arguments.of("reqItemList 명시적 null", 400, """{"reqItemList": null}"""),
            Arguments.of("reqItemList 빈 배열", 422, """{"reqItemList": []}"""),
            Arguments.of("malformed JSON", 400, """{"reqItemList": ["""),
            Arguments.of("reqItemList 가 array 아닌 type", 400, """{"reqItemList": "not-array"}""")
        )
    }
}
