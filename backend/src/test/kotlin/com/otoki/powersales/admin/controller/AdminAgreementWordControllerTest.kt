package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.support.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.domain.support.agreement.dto.response.AdminAgreementWordActiveResponse
import com.otoki.powersales.domain.support.agreement.dto.response.AdminAgreementWordCreateResponse
import com.otoki.powersales.domain.support.agreement.service.AdminAgreementWordService
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminAgreementWordController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAgreementWordController 테스트 (Spec #658 P1-B)")
class AdminAgreementWordControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminAgreementWordService: AdminAgreementWordService

    private val futureDate: LocalDate = LocalDate.now().plusMonths(6)

    @BeforeEach
    fun setUpSystemAdminPrincipal() {
        authenticateAsAdmin(role = null)
    }

    @Nested
    @DisplayName("POST /api/v1/admin/agreement-words - 신규 약관 등록")
    inner class CreateAgreementWord {

        @Test
        @DisplayName("T1 성공 - 정상 등록 (201 Created + active=false / activeDate=null)")
        fun t1_success() {
            val request = AdminAgreementWordCreateRequest(
                name = "AGR-2026-001",
                contents = "위치정보 수집·이용 동의서",
                afterActiveDate = futureDate
            )
            val response = AdminAgreementWordCreateResponse(
                agreementWordId = 12,
                name = "AGR-2026-001",
                afterActiveDate = futureDate,
                active = false,
                activeDate = null,
                createdAt = LocalDateTime.of(2026, 5, 11, 14, 23, 51)
            )
            every { adminAgreementWordService.createAgreementWord(any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreementWordId").value(12))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.message").value("약관 등록 성공"))
        }

        // T2 active=true / T3 activeDate 입력 / T6 name 누락 / T7 afterActiveDate 누락 /
        // T8 afterActiveDate 과거 / T10 name 81자 / T11 contents 8001자 — 모두 @Valid → 400 INVALID_PARAMETER 매핑
        // (controller dispatcher 책임만 검증, raw JSON 으로 validator 검증)
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminAgreementWordControllerTest#validationFailures")
        @DisplayName("실패 - @Valid 위반 → 400 INVALID_PARAMETER")
        fun create_validationFailures(@Suppress("UNUSED_PARAMETER") name: String, rawJson: String) {
            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        // T4 (AGREEMENT_WRITE 권한 미보유 → 403) / T5 (인증 누락 → 401) 는 WebAdminContextFilter 단에서 처리 — 본 컨트롤러 테스트 책임 외.

        @Test
        @DisplayName("T9 응답 wrap 형식 검증 — ApiResponse<T> 구조 정합")
        fun t9_apiResponseFormat() {
            val request = AdminAgreementWordCreateRequest(
                name = "AGR-2026-001",
                contents = "위치정보 수집·이용 동의서",
                afterActiveDate = futureDate
            )
            val response = AdminAgreementWordCreateResponse(
                agreementWordId = 12,
                name = "AGR-2026-001",
                afterActiveDate = futureDate,
                active = false,
                activeDate = null,
                createdAt = LocalDateTime.of(2026, 5, 11, 14, 23, 51)
            )
            every { adminAgreementWordService.createAgreementWord(any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists())
        }

        @Test
        @DisplayName("T12 active 필드 미입력 → 201 (Service 단 fallback 으로 false)")
        fun t12_activeOmittedDefaultsFalse() {
            val response = AdminAgreementWordCreateResponse(
                agreementWordId = 12,
                name = "AGR-2026-001",
                afterActiveDate = futureDate,
                active = false,
                activeDate = null,
                createdAt = LocalDateTime.of(2026, 5, 11, 14, 23, 51)
            )
            every { adminAgreementWordService.createAgreementWord(any()) } returns response

            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.active").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/agreement-words/active - 활성 약관 조회")
    inner class GetActive {

        @Test
        @DisplayName("T13 정상 조회 — 활성 약관 1건 반환")
        fun t13_activeFound() {
            val today = LocalDate.now()
            val response = AdminAgreementWordActiveResponse(
                agreementWordId = 11,
                name = "AGR-2025-002",
                contents = "위치정보 수집·이용 동의서",
                activeDate = today,
                afterActiveDate = today.plusMonths(6)
            )
            every { adminAgreementWordService.getActiveAgreementWord() } returns response

            mockMvc.perform(get("/api/v1/admin/agreement-words/active"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.agreementWordId").value(11))
                .andExpect(jsonPath("$.data.name").value("AGR-2025-002"))
        }

        @Test
        @DisplayName("T14 활성 약관 부재 → 200 OK + data: null + message: '활성 약관 없음'")
        fun t14_activeAbsent() {
            every { adminAgreementWordService.getActiveAgreementWord() } returns null

            mockMvc.perform(get("/api/v1/admin/agreement-words/active"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("활성 약관 없음"))
        }

        // T15 (AGREEMENT_READ 권한 미보유 → 403) 는 WebAdminContextFilter 단에서 처리.
    }

    companion object {
        @JvmStatic
        fun validationFailures(): List<Arguments> {
            val future = LocalDate.now().plusMonths(6)
            val name81 = "A".repeat(81)
            val contents8001 = "C".repeat(8001)
            return listOf(
                Arguments.of(
                    "T2 active=true 입력 차단 (@AssertFalse 위반)",
                    """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$future","active":true}""",
                ),
                Arguments.of(
                    "T3 activeDate 입력 차단 (@Null 위반)",
                    """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$future","activeDate":"$future"}""",
                ),
                Arguments.of("T6 name 누락 (@NotBlank 위반)", """{"name":"","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$future"}"""),
                Arguments.of("T7 afterActiveDate 누락 (@NotNull 위반)", """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서"}"""),
                Arguments.of(
                    "T8 afterActiveDate 과거 일자 (@Future 위반)",
                    """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"2025-01-01"}""",
                ),
                Arguments.of("T10 name 길이 81자 (@Size 위반)", """{"name":"$name81","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$future"}"""),
                Arguments.of("T11 contents 길이 8001자 (@Size 위반)", """{"name":"AGR-2026-001","contents":"$contents8001","afterActiveDate":"$future"}"""),
            )
        }
    }
}
