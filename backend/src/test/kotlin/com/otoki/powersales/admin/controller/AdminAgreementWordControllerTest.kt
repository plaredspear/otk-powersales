package com.otoki.powersales.admin.controller

import com.otoki.powersales.agreement.dto.request.AdminAgreementWordCreateRequest
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordActiveResponse
import com.otoki.powersales.agreement.dto.response.AdminAgreementWordCreateResponse
import com.otoki.powersales.agreement.service.AdminAgreementWordService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
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
class AdminAgreementWordControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminAgreementWordService: AdminAgreementWordService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter


    private val futureDate: LocalDate = LocalDate.now().plusMonths(6)

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.SYSTEM_ADMIN,
            costCenterCode = null,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
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
            whenever(adminAgreementWordService.createAgreementWord(any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreementWordId").value(12))
                .andExpect(jsonPath("$.data.name").value("AGR-2026-001"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.activeDate").doesNotExist())
                .andExpect(jsonPath("$.data.afterActiveDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.message").value("약관 등록 성공"))
        }

        @Test
        @DisplayName("T2 active=true 입력 차단 → 400 INVALID_PARAMETER (@AssertFalse 위반)")
        fun t2_activeTrueRejected() {
            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate","active":true}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("T3 activeDate 입력 차단 → 400 INVALID_PARAMETER (@Null 위반)")
        fun t3_activeDateRejected() {
            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate","activeDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        // T4 (AGREEMENT_WRITE 권한 미보유 → 403) / T5 (인증 누락 → 401) 는 WebAdminContextFilter 단에서 처리.
        // 본 컨트롤러 테스트는 @AutoConfigureMockMvc(addFilters = false) 로 필터를 우회하므로
        // 권한/인증 케이스는 컨트롤러 테스트 책임 외.

        @Test
        @DisplayName("T6 name 누락 → 400 INVALID_PARAMETER (@NotBlank 위반)")
        fun t6_nameBlank() {
            val rawJson = """{"name":"","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("T7 afterActiveDate 누락 → 400 INVALID_PARAMETER (@NotNull 위반)")
        fun t7_afterActiveDateNull() {
            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("T8 afterActiveDate 과거 일자 → 400 INVALID_PARAMETER (@Future 위반)")
        fun t8_afterActiveDatePast() {
            val pastDate = LocalDate.of(2025, 1, 1)
            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$pastDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

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
            whenever(adminAgreementWordService.createAgreementWord(any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists())
        }

        @Test
        @DisplayName("T10 name 길이 81자 → 400 INVALID_PARAMETER (@Size(max=80) 위반)")
        fun t10_nameTooLong() {
            val name81 = "A".repeat(81)
            val rawJson = """{"name":"$name81","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("T11 contents 길이 8001자 → 400 INVALID_PARAMETER (@Size(max=8000) 위반)")
        fun t11_contentsTooLong() {
            val contents8001 = "C".repeat(8001)
            val rawJson = """{"name":"AGR-2026-001","contents":"$contents8001","afterActiveDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
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
            whenever(adminAgreementWordService.createAgreementWord(any())).thenReturn(response)

            val rawJson = """{"name":"AGR-2026-001","contents":"위치정보 수집·이용 동의서","afterActiveDate":"$futureDate"}"""

            mockMvc.perform(
                post("/api/v1/admin/agreement-words")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
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
            whenever(adminAgreementWordService.getActiveAgreementWord()).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/agreement-words/active"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreementWordId").value(11))
                .andExpect(jsonPath("$.data.name").value("AGR-2025-002"))
                .andExpect(jsonPath("$.data.contents").value("위치정보 수집·이용 동의서"))
                .andExpect(jsonPath("$.data.activeDate").value(today.toString()))
                .andExpect(jsonPath("$.data.afterActiveDate").value(today.plusMonths(6).toString()))
        }

        @Test
        @DisplayName("T14 활성 약관 부재 → 200 OK + data: null + message: '활성 약관 없음'")
        fun t14_activeAbsent() {
            whenever(adminAgreementWordService.getActiveAgreementWord()).thenReturn(null)

            mockMvc.perform(get("/api/v1/admin/agreement-words/active"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("활성 약관 없음"))
        }

        // T15 (AGREEMENT_READ 권한 미보유 → 403) 는 WebAdminContextFilter 단에서 처리.
        // 본 컨트롤러 테스트는 @AutoConfigureMockMvc(addFilters = false) 로 필터를 우회.
    }
}
