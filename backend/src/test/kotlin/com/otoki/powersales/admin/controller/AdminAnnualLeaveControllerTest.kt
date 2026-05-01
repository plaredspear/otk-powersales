package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.AnnualLeaveDayDto
import com.otoki.powersales.admin.dto.response.EmployeeAnnualLeaveDto
import com.otoki.powersales.admin.service.AdminAnnualLeaveService
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAnnualLeaveController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAnnualLeaveController 테스트")
class AdminAnnualLeaveControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminAnnualLeaveService: AdminAnnualLeaveService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

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
    @DisplayName("GET /api/v1/admin/annual-leave/summary - 연차 현황 조회")
    inner class GetSummary {

        @Test
        @DisplayName("성공 - 유효한 yearMonth → 200 응답")
        fun validYearMonth_returns200() {
            val data = listOf(
                EmployeeAnnualLeaveDto(
                    employeeCode = "EMP001",
                    employeeName = "홍길동",
                    orgName = "서울1팀",
                    annualLeaveDays = listOf(
                        AnnualLeaveDayDto(date = "2026-03-05", attendTypeName = "연차"),
                        AnnualLeaveDayDto(date = "2026-03-10", attendTypeName = "연차")
                    ),
                    totalCount = 2
                )
            )
            whenever(adminAnnualLeaveService.getSummary(eq("2026-03"), isNull()))
                .thenReturn(data)

            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "2026-03")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employee_code").value("EMP001"))
                .andExpect(jsonPath("$.data[0].employee_name").value("홍길동"))
                .andExpect(jsonPath("$.data[0].org_name").value("서울1팀"))
                .andExpect(jsonPath("$.data[0].annual_leave_days[0].date").value("2026-03-05"))
                .andExpect(jsonPath("$.data[0].annual_leave_days[0].attend_type_name").value("연차"))
                .andExpect(jsonPath("$.data[0].total_count").value(2))
        }

        @Test
        @DisplayName("실패 - 잘못된 yearMonth 형식 '202603' → 400 응답")
        fun invalidYearMonthFormat_returns400() {
            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "202603")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("성공 - orgCode 파라미터 포함 → 200 응답")
        fun withOrgCode_returns200() {
            val data = listOf(
                EmployeeAnnualLeaveDto(
                    employeeCode = "EMP001",
                    employeeName = "홍길동",
                    orgName = "서울1팀",
                    annualLeaveDays = listOf(
                        AnnualLeaveDayDto(date = "2026-03-05", attendTypeName = "연차")
                    ),
                    totalCount = 1
                )
            )
            whenever(adminAnnualLeaveService.getSummary(eq("2026-03"), eq("서울1팀")))
                .thenReturn(data)

            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "2026-03")
                    .param("orgCode", "서울1팀")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employee_code").value("EMP001"))
                .andExpect(jsonPath("$.data[0].org_name").value("서울1팀"))
                .andExpect(jsonPath("$.data[0].total_count").value(1))
        }
    }
}
