package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.BranchDto
import com.otoki.internal.admin.service.AdminScheduleService
import com.otoki.internal.admin.service.MissingCostCenterCodeException
import com.otoki.internal.admin.service.OrganizationNotFoundException
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminScheduleController 테스트")
class AdminScheduleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminScheduleService: AdminScheduleService

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
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedule/branches - 지점 목록 조회")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 지점 목록 반환")
        fun getBranches_success() {
            val branches = listOf(
                BranchDto("1234", "서울지점"),
                BranchDto("5678", "부산지점")
            )
            whenever(adminScheduleService.getBranches()).thenReturn(branches)

            mockMvc.perform(get("/api/v1/admin/schedule/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].cost_center_code").value("1234"))
                .andExpect(jsonPath("$.data[0].branch_name").value("서울지점"))
                .andExpect(jsonPath("$.data[1].cost_center_code").value("5678"))
                .andExpect(jsonPath("$.data[1].branch_name").value("부산지점"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedule/template - 양식 다운로드")
    inner class DownloadTemplate {

        @Test
        @DisplayName("성공 - Excel 파일 다운로드")
        fun downloadTemplate_success() {
            val result = AdminScheduleService.TemplateResult(
                bytes = ByteArray(100),
                filename = "진열스케줄_양식_1234_20260314120000.xlsx"
            )
            whenever(adminScheduleService.generateTemplate("1234")).thenReturn(result)

            mockMvc.perform(
                get("/api/v1/admin/schedule/template")
                    .param("costCenterCode", "1234")
            )
                .andExpect(status().isOk)
                .andExpect(
                    header().string(
                        "Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                )
                .andExpect(header().exists("Content-Disposition"))
        }

        @Test
        @DisplayName("실패 - cost_center_code 파라미터 누락")
        fun downloadTemplate_missingParam() {
            mockMvc.perform(get("/api/v1/admin/schedule/template"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 지점 코드")
        fun downloadTemplate_orgNotFound() {
            whenever(adminScheduleService.generateTemplate("0000"))
                .thenThrow(OrganizationNotFoundException())

            mockMvc.perform(
                get("/api/v1/admin/schedule/template")
                    .param("costCenterCode", "0000")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_NOT_FOUND"))
        }
    }
}
