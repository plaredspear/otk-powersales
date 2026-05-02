package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.BasicStats
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.dto.response.PreviousMonthData
import com.otoki.powersales.admin.dto.response.SalesSummary
import com.otoki.powersales.admin.dto.response.StaffDeployment
import com.otoki.powersales.admin.dto.response.StaffTypeCount
import com.otoki.powersales.admin.dto.response.TotalByPosition
import com.otoki.powersales.admin.dto.response.WorkTypeStats
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminDashboardController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminDashboardController 테스트 (스텁 모드)")
class AdminDashboardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminDashboardService: AdminDashboardService

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

    @MockitoBean
    private lateinit var dataScopeHolder: DataScopeHolder

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.BRANCH_MANAGER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun emptyDashboardResponse(yearMonth: String): DashboardResponse = DashboardResponse(
        salesSummary = SalesSummary(
            yearMonth = yearMonth,
            branchName = null,
            targetAmount = 0L,
            actualAmount = 0L,
            progressRate = 0.0,
            referenceProgressRate = 0.0,
            lastYearAmount = 0L,
            lastYearRatio = 0.0,
            channelSales = emptyList()
        ),
        staffDeployment = StaffDeployment(
            yearMonth = yearMonth,
            branchName = null,
            byAccountType = emptyList(),
            byWorkType = emptyList(),
            byChannelAndWorkType = emptyList(),
            previousMonth = PreviousMonthData(byWorkType = emptyList())
        ),
        basicStats = BasicStats(
            branchName = null,
            staffType = StaffTypeCount(promotion = 0, osc = 0),
            totalByPosition = TotalByPosition(active = 0, onLeave = 0),
            byAgeGroup = emptyList(),
            byWorkType = WorkTypeStats(fixed = 0, alternating = 0, visiting = 0)
        )
    )

    @Nested
    @DisplayName("GET /api/v1/admin/dashboard - 대시보드 조회")
    inner class GetDashboard {

        @Test
        @DisplayName("성공 - 200 OK + 응답 스키마 키 모두 존재")
        fun getDashboard_success_schemaKeysExist() {
            whenever(adminDashboardService.getDashboard(anyOrNull(), anyOrNull()))
                .thenReturn(emptyDashboardResponse("2026-03"))

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-03")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("대시보드 조회 성공"))
                .andExpect(jsonPath("$.data.sales_summary").exists())
                .andExpect(jsonPath("$.data.sales_summary.year_month").value("2026-03"))
                .andExpect(jsonPath("$.data.sales_summary.branch_name").doesNotExist())
                .andExpect(jsonPath("$.data.sales_summary.target_amount").value(0))
                .andExpect(jsonPath("$.data.sales_summary.actual_amount").value(0))
                .andExpect(jsonPath("$.data.sales_summary.progress_rate").value(0.0))
                .andExpect(jsonPath("$.data.sales_summary.reference_progress_rate").value(0.0))
                .andExpect(jsonPath("$.data.sales_summary.last_year_amount").value(0))
                .andExpect(jsonPath("$.data.sales_summary.last_year_ratio").value(0.0))
                .andExpect(jsonPath("$.data.sales_summary.channel_sales").isArray)
                .andExpect(jsonPath("$.data.sales_summary.channel_sales").isEmpty)
                .andExpect(jsonPath("$.data.staff_deployment").exists())
                .andExpect(jsonPath("$.data.staff_deployment.year_month").value("2026-03"))
                .andExpect(jsonPath("$.data.staff_deployment.by_account_type").isArray)
                .andExpect(jsonPath("$.data.staff_deployment.by_account_type").isEmpty)
                .andExpect(jsonPath("$.data.staff_deployment.by_work_type").isArray)
                .andExpect(jsonPath("$.data.staff_deployment.by_work_type").isEmpty)
                .andExpect(jsonPath("$.data.staff_deployment.by_channel_and_work_type").isArray)
                .andExpect(jsonPath("$.data.staff_deployment.by_channel_and_work_type").isEmpty)
                .andExpect(jsonPath("$.data.staff_deployment.previous_month.by_work_type").isArray)
                .andExpect(jsonPath("$.data.staff_deployment.previous_month.by_work_type").isEmpty)
                .andExpect(jsonPath("$.data.basic_stats").exists())
                .andExpect(jsonPath("$.data.basic_stats.staff_type.promotion").value(0))
                .andExpect(jsonPath("$.data.basic_stats.staff_type.osc").value(0))
                .andExpect(jsonPath("$.data.basic_stats.total_by_position.active").value(0))
                .andExpect(jsonPath("$.data.basic_stats.total_by_position.on_leave").value(0))
                .andExpect(jsonPath("$.data.basic_stats.by_age_group").isArray)
                .andExpect(jsonPath("$.data.basic_stats.by_age_group").isEmpty)
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.fixed").value(0))
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.alternating").value(0))
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.visiting").value(0))
        }

        @Test
        @DisplayName("성공 - yearMonth 미입력 시 응답의 year_month가 YYYY-MM 패턴")
        fun getDashboard_success_noYearMonth() {
            whenever(adminDashboardService.getDashboard(anyOrNull(), anyOrNull()))
                .thenReturn(emptyDashboardResponse("2026-05"))

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sales_summary.year_month").value("2026-05"))
        }

        @Test
        @DisplayName("실패 - yearMonth 형식 위반 (구분자 없음) -> 400 VALIDATION_ERROR")
        fun getDashboard_invalidYearMonthFormat() {
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "202603")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("실패 - yearMonth 월 범위 초과 -> 400 VALIDATION_ERROR")
        fun getDashboard_invalidYearMonthRange() {
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-13")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }
    }
}
