package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.BasicStats
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.dto.response.PreviousMonthData
import com.otoki.powersales.admin.dto.response.SalesSummary
import com.otoki.powersales.admin.dto.response.StaffDeployment
import com.otoki.powersales.admin.dto.response.StaffTypeCount
import com.otoki.powersales.admin.dto.response.TotalByPosition
import com.otoki.powersales.admin.dto.response.WorkTypeStats
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.schedule.service.AdminTeamScheduleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminDashboardController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminDashboardController 테스트")
class AdminDashboardControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminDashboardService: AdminDashboardService

    @MockkBean
    private lateinit var adminTeamScheduleService: AdminTeamScheduleService

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUpArgResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
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
            every { adminDashboardService.getDashboard(any(), any(), any()) } returns emptyDashboardResponse("2026-03")

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-03")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("대시보드 조회 성공"))
                .andExpect(jsonPath("$.data.salesSummary").exists())
                .andExpect(jsonPath("$.data.salesSummary.yearMonth").value("2026-03"))
                .andExpect(jsonPath("$.data.salesSummary.branchName").doesNotExist())
                .andExpect(jsonPath("$.data.salesSummary.targetAmount").value(0))
                .andExpect(jsonPath("$.data.salesSummary.actualAmount").value(0))
                .andExpect(jsonPath("$.data.salesSummary.progressRate").value(0.0))
                .andExpect(jsonPath("$.data.salesSummary.referenceProgressRate").value(0.0))
                .andExpect(jsonPath("$.data.salesSummary.lastYearAmount").value(0))
                .andExpect(jsonPath("$.data.salesSummary.lastYearRatio").value(0.0))
                .andExpect(jsonPath("$.data.salesSummary.channelSales").isArray)
                .andExpect(jsonPath("$.data.salesSummary.channelSales").isEmpty)
                .andExpect(jsonPath("$.data.staffDeployment").exists())
                .andExpect(jsonPath("$.data.staffDeployment.yearMonth").value("2026-03"))
                .andExpect(jsonPath("$.data.staffDeployment.byAccountType").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.byAccountType").isEmpty)
                .andExpect(jsonPath("$.data.staffDeployment.byWorkType").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.byWorkType").isEmpty)
                .andExpect(jsonPath("$.data.staffDeployment.byChannelAndWorkType").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.byChannelAndWorkType").isEmpty)
                .andExpect(jsonPath("$.data.staffDeployment.previousMonth.byWorkType").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.previousMonth.byWorkType").isEmpty)
                .andExpect(jsonPath("$.data.basicStats").exists())
                .andExpect(jsonPath("$.data.basicStats.staffType.promotion").value(0))
                .andExpect(jsonPath("$.data.basicStats.staffType.osc").value(0))
                .andExpect(jsonPath("$.data.basicStats.totalByPosition.active").value(0))
                .andExpect(jsonPath("$.data.basicStats.totalByPosition.onLeave").value(0))
                .andExpect(jsonPath("$.data.basicStats.byAgeGroup").isArray)
                .andExpect(jsonPath("$.data.basicStats.byAgeGroup").isEmpty)
                .andExpect(jsonPath("$.data.basicStats.byWorkType.fixed").value(0))
                .andExpect(jsonPath("$.data.basicStats.byWorkType.alternating").value(0))
                .andExpect(jsonPath("$.data.basicStats.byWorkType.visiting").value(0))
        }

        @Test
        @DisplayName("성공 - yearMonth 미입력 시 응답의 year_month가 YYYY-MM 패턴")
        fun getDashboard_success_noYearMonth() {
            every { adminDashboardService.getDashboard(any(), any(), any()) } returns emptyDashboardResponse("2026-05")

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.salesSummary.yearMonth").value("2026-05"))
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

    @Nested
    @DisplayName("GET /api/v1/admin/dashboard/branches - 대시보드 지점 셀렉터")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 200 OK + 지점 목록 반환")
        fun getBranches_success() {
            val branches = listOf(
                BranchResponse(branchCode = "1234", branchName = "서울지점"),
                BranchResponse(branchCode = "5678", branchName = "부산지점")
            )
            every { adminTeamScheduleService.getBranches(any()) } returns branches

            mockMvc.perform(get("/api/v1/admin/dashboard/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].branchCode").value("1234"))
                .andExpect(jsonPath("$.data[0].branchName").value("서울지점"))
        }
    }
}
