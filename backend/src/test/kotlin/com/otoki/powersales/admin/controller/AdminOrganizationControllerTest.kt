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
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
import java.time.LocalDateTime

@WebMvcTest(AdminOrganizationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminOrganizationController 테스트")
class AdminOrganizationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var adminOrganizationService: AdminOrganizationService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    // @AutoConfigureMockMvc(addFilters = false) 환경에서 WebAdminContextFilter 가 동작하지 않으므로
    // ArgumentResolver 자체를 stub 하여 ALL scope 기본값 주입.
    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
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
        @DisplayName("성공 - 기본 조회")
        fun getOrganizations_success() {
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].costCenterLevel2").value("1000"))
                .andExpect(jsonPath("$.data[0].orgCodeLevel2").value("A100"))
                .andExpect(jsonPath("$.data[0].orgNameLevel2").value("영업본부"))
                .andExpect(jsonPath("$.data[0].costCenterLevel4").value("1101"))
                .andExpect(jsonPath("$.data[0].orgNameLevel4").value("강남지점"))
                .andExpect(jsonPath("$.data[0].costCenterLevel5").value("1101A"))
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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getOrganizations_empty() {
            every { adminOrganizationService.getOrganizations(any(), any(), any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/admin/organizations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 level")
        fun getOrganizations_invalidLevel() {
            mockMvc.perform(
                get("/api/v1/admin/organizations")
                    .param("level", "L6")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - keyword 50자 초과")
        fun getOrganizations_keywordTooLong() {
            val longKeyword = "가".repeat(51)
            mockMvc.perform(
                get("/api/v1/admin/organizations")
                    .param("keyword", longKeyword)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
