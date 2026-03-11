package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.OrgResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminOrganizationService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
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

    @MockitoBean
    private lateinit var adminOrganizationService: AdminOrganizationService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean
    private lateinit var dataScopeHolder: DataScopeHolder

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/organizations - 조직마스터 목록 조회")
    inner class GetOrganizations {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getOrganizations_success() {
            val response = listOf(
                OrgResponse(
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
                    createdAt = LocalDateTime.of(2026, 1, 15, 9, 0)
                )
            )
            whenever(adminOrganizationService.getOrganizations(anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/organizations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].cost_center_level2").value("1000"))
                .andExpect(jsonPath("$.data[0].org_code_level2").value("A100"))
                .andExpect(jsonPath("$.data[0].org_name_level2").value("영업본부"))
                .andExpect(jsonPath("$.data[0].cost_center_level4").value("1101"))
                .andExpect(jsonPath("$.data[0].org_name_level4").value("강남지점"))
                .andExpect(jsonPath("$.data[0].cost_center_level5").value("1101A"))
                .andExpect(jsonPath("$.data[0].org_name_level5").value("강남1조"))
        }

        @Test
        @DisplayName("성공 - 키워드 + 레벨 필터")
        fun getOrganizations_withFilters() {
            whenever(adminOrganizationService.getOrganizations(eq("강남"), eq("L4")))
                .thenReturn(emptyList())

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
            whenever(adminOrganizationService.getOrganizations(anyOrNull(), anyOrNull()))
                .thenReturn(emptyList())

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
