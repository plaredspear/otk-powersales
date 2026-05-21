package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.organization.dto.response.OrganizationResponse
import com.otoki.powersales.organization.service.AdminOrganizationService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.auth.entity.UserRoleEnum
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminOrganizationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminOrganizationController 테스트")
class AdminOrganizationControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var adminOrganizationService: AdminOrganizationService
    @MockkBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockkBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockkBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockkBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRoleEnum.BRANCH_MANAGER,
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
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/organizations - 조직마스터 목록 조회")
    inner class GetOrganizations {

        @Test
        @DisplayName("성공 - 기본 조회 (계층 구조 5 레벨 응답)")
        fun getOrganizations_success() {
            // controller 응답 - 조직 계층 5 레벨이 정상 매핑되는지 확인 (가드레일 5.3 — 비즈니스 의미: 계층 구조)
            val response = listOf(
                OrganizationResponse(
                    id = 1L,
                    costCenterLevel2 = "1000",
                    orgCodeLevel2 = "A100",
                    orgNameLevel2 = "영업본부",
                    costCenterLevel3 = "1100",
                    orgCodeLevel3 = "A110",
                    orgNameLevel3 = "수도권사업부",
                    costCenterLevel4 = "1101",
                    orgCodeLevel4 = "A111",
                    orgNameLevel4 = "강남지점",
                    costCenterLevel5 = "1101A",
                    orgCodeLevel5 = "A111A",
                    orgNameLevel5 = "강남1조",
                    createdAt = java.time.LocalDateTime.of(2026, 1, 15, 9, 0)
                )
            )
            every { adminOrganizationService.getOrganizations(any(), any(), any()) } returns response

            mockMvc.perform(get("/api/v1/admin/organizations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].orgNameLevel2").value("영업본부"))
                .andExpect(jsonPath("$.data[0].orgNameLevel5").value("강남1조"))
        }

        @Test
        @DisplayName("성공 - 키워드 + 레벨 필터")
        fun getOrganizations_withFilters() {
            every { adminOrganizationService.getOrganizations(any(), eq("강남"), eq("L4")) } returns emptyList()

            mockMvc.perform(
                get("/api/v1/admin/organizations")
                    .param("keyword", "강남")
                    .param("level", "L4")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getOrganizations_empty() {
            every { adminOrganizationService.getOrganizations(any(), any(), any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/admin/organizations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminOrganizationControllerTest#validationFailures")
        @DisplayName("실패 - @Valid 위반 → 400")
        fun getOrganizations_validationFailures(@Suppress("UNUSED_PARAMETER") name: String, paramName: String, paramValue: String) {
            mockMvc.perform(
                get("/api/v1/admin/organizations").param(paramName, paramValue)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    companion object {
        @JvmStatic
        fun validationFailures(): List<org.junit.jupiter.params.provider.Arguments> = listOf(
            org.junit.jupiter.params.provider.Arguments.of("invalidLevel - L6 → 400", "level", "L6"),
            org.junit.jupiter.params.provider.Arguments.of("keywordTooLong - 51자 → 400", "keyword", "가".repeat(51)),
        )
    }
}
