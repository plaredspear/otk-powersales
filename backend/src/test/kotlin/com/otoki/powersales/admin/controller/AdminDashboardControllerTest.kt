package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.BasicStats
import com.otoki.powersales.admin.dto.response.BasicStatsByScope
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.dto.response.SalesSummary
import com.otoki.powersales.admin.dto.response.StaffDeployment
import com.otoki.powersales.admin.dto.response.StaffTypeCount
import com.otoki.powersales.admin.dto.response.TotalByPosition
import com.otoki.powersales.admin.dto.response.WorkTypeChannelChart
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import java.math.BigDecimal
import java.time.LocalDate
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
    private lateinit var branchScopeGateway: BranchScopeGateway

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
        // 지점 스코프 방식(개발자 도구 토글) — 기본 UNIFIED. 응답 branchScopeMode 에 실린다.
        every { branchScopeGateway.currentMode() } returns BranchScopeMode.UNIFIED
    }

    private fun emptyChart() =
        WorkTypeChannelChart(stackKeys = emptyList(), rows = emptyList(), totalHeadcount = BigDecimal.ZERO)

    /** 집계 기준(재직 / 재직+휴직) 한쪽의 빈 수치 묶음. */
    private fun emptyBasicStatsByScope() = BasicStatsByScope(
        staffType = StaffTypeCount(promotion = 0, osc = 0, etc = 0, etcBreakdown = emptyList()),
        byAgeGroup = emptyList(),
        averageAge = null,
        byRank = emptyList(),
    )

    private fun emptyDashboardResponse(yearMonth: String): DashboardResponse = DashboardResponse(
        salesSummary = SalesSummary(
            yearMonth = yearMonth,
            branchName = null,
            investedAccountCount = 0,
            targetAmount = 0L,
            actualAmount = 0L,
            progressRate = 0.0,
            referenceProgressRate = 0.0,
            lastYearAmount = 0L,
            lastYearRatio = 0.0,
            channelSales = emptyList(),
            hasActualData = false,
            hasLastYearData = false,
            hasTargetData = false
        ),
        staffDeployment = StaffDeployment(
            yearMonth = yearMonth,
            branchName = null,
            byAccountType = emptyList(),
            channelWorkType1 = emptyChart(),
            workType1Ratio = emptyList(),
            all = emptyChart(),
            display = emptyChart(),
            event = emptyChart()
        ),
        basicStats = BasicStats(
            branchName = null,
            // 집계 기준 토글 — 두 기준을 모두 내려준다 (화면이 전환, 재조회 없음)
            active = emptyBasicStatsByScope(),
            includingLeave = emptyBasicStatsByScope(),
            // 토글과 무관하게 항상 전체 기준
            totalByPosition = TotalByPosition(active = 0, onLeave = 0, etc = 0, etcBreakdown = emptyList()),
            asOfDate = LocalDate.of(2026, 5, 19),
        ),
        branchScopeMode = "UNIFIED",
    )

    @Nested
    @DisplayName("GET /api/v1/admin/dashboard - 대시보드 조회")
    inner class GetDashboard {

        @Test
        @DisplayName("성공 - 200 OK + 응답 스키마 키 모두 존재")
        fun getDashboard_success_schemaKeysExist() {
            every { branchScopeGateway.resolveBranches(any(), any()) } returns emptyList()
            every { branchScopeGateway.resolveScope(any(), any<List<String>>(), any()) } returns
                BranchScopeResult.Allowed(grantedCodes = emptyList(), queryCodes = emptyList())
            every { adminDashboardService.getDashboard(any(), any(), any(), any(), any()) } returns emptyDashboardResponse("2026-03")

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
                .andExpect(jsonPath("$.data.staffDeployment.channelWorkType1.rows").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.workType1Ratio").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.all.rows").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.display.stackKeys").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.display.rows").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.display.rows").isEmpty)
                .andExpect(jsonPath("$.data.staffDeployment.event.stackKeys").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.event.rows").isArray)
                .andExpect(jsonPath("$.data.staffDeployment.event.rows").isEmpty)
                .andExpect(jsonPath("$.data.basicStats").exists())
                // 집계 기준 토글 — 두 기준이 모두 내려온다 (화면이 전환, 재조회 없음)
                .andExpect(jsonPath("$.data.basicStats.active.staffType.promotion").value(0))
                .andExpect(jsonPath("$.data.basicStats.active.staffType.osc").value(0))
                .andExpect(jsonPath("$.data.basicStats.includingLeave.staffType.promotion").value(0))
                .andExpect(jsonPath("$.data.basicStats.active.byAgeGroup").isArray)
                .andExpect(jsonPath("$.data.basicStats.active.byAgeGroup").isEmpty)
                // 총원 카드는 토글 밖 (기준별로 나뉘지 않음)
                .andExpect(jsonPath("$.data.basicStats.totalByPosition.active").value(0))
                .andExpect(jsonPath("$.data.basicStats.totalByPosition.onLeave").value(0))
                // byWorkType(근무형태별 환산인원) 은 기준 시점 혼선으로 기본 현황에서 제거됨 —
                // 근무형태별 집계는 여사원 투입현황(staffDeployment) 담당.
                .andExpect(jsonPath("$.data.basicStats.byWorkType").doesNotExist())
        }

        @Test
        @DisplayName("성공 - yearMonth 미입력 시 응답의 year_month가 YYYY-MM 패턴")
        fun getDashboard_success_noYearMonth() {
            every { branchScopeGateway.resolveBranches(any(), any()) } returns emptyList()
            every { branchScopeGateway.resolveScope(any(), any<List<String>>(), any()) } returns
                BranchScopeResult.Allowed(grantedCodes = emptyList(), queryCodes = emptyList())
            every { adminDashboardService.getDashboard(any(), any(), any(), any(), any()) } returns emptyDashboardResponse("2026-05")

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.salesSummary.yearMonth").value("2026-05"))
        }

        @Test
        @DisplayName("지점 스코프 전달 - Allowed 의 grantedCodes(라벨)/queryCodes(필터)가 service 로 그대로 전달")
        fun getDashboard_forwardsGrantedAndQueryCodes() {
            every { branchScopeGateway.resolveBranches(any(), any()) } returns emptyList()
            every { branchScopeGateway.resolveScope(any(), listOf("5824"), any()) } returns
                BranchScopeResult.Allowed(grantedCodes = listOf("5824"), queryCodes = listOf("5824", "5668"))
            every { adminDashboardService.getDashboard(any(), any(), any(), any(), any()) } returns emptyDashboardResponse("2026-05")

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("branchCode", "5824")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk)

            io.mockk.verify {
                adminDashboardService.getDashboard(listOf("5824"), null, any(), listOf("5824", "5668"), any())
            }
        }

        @Test
        @DisplayName("지점 스코프 차단 - NoAccess 는 빈 문자열 sentinel 로 전달 (전건 조회로 새지 않음)")
        fun getDashboard_noAccessSentinel() {
            every { branchScopeGateway.resolveBranches(any(), any()) } returns emptyList()
            every { branchScopeGateway.resolveScope(any(), any<List<String>>(), any()) } returns BranchScopeResult.NoAccess
            every { adminDashboardService.getDashboard(any(), any(), any(), any(), any()) } returns emptyDashboardResponse("2026-05")

            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("branchCode", "9999")
                    .contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk)

            io.mockk.verify {
                adminDashboardService.getDashboard(listOf(""), null, any(), listOf(""), any())
            }
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
            every { branchScopeGateway.resolveBranches(any(), any()) } returns branches

            mockMvc.perform(get("/api/v1/admin/dashboard/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].branchCode").value("1234"))
                .andExpect(jsonPath("$.data[0].branchName").value("서울지점"))
        }
    }
}
