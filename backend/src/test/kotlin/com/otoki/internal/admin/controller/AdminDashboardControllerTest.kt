package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminDashboardService
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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
@DisplayName("AdminDashboardController 테스트")
class AdminDashboardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminDashboardService: AdminDashboardService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun buildSampleDashboardResponse(
        yearMonth: String = "2026-03",
        branchName: String? = "서울지점"
    ): DashboardResponse {
        return DashboardResponse(
            salesSummary = SalesSummary(
                yearMonth = yearMonth,
                branchName = branchName,
                targetAmount = 500_000_000L,
                actualAmount = 420_000_000L,
                progressRate = 84.0,
                referenceProgressRate = 80.0,
                lastYearAmount = 400_000_000L,
                lastYearRatio = 105.0,
                channelSales = listOf(
                    ChannelSalesItem(
                        channelName = "대형마트",
                        targetAmount = 200_000_000L,
                        actualAmount = 180_000_000L,
                        progressRate = 90.0
                    ),
                    ChannelSalesItem(
                        channelName = "편의점",
                        targetAmount = 150_000_000L,
                        actualAmount = 120_000_000L,
                        progressRate = 80.0
                    )
                )
            ),
            staffDeployment = StaffDeployment(
                yearMonth = yearMonth,
                branchName = branchName,
                byAccountType = listOf(
                    AccountTypeCount(accountType = "직영", count = 30),
                    AccountTypeCount(accountType = "위탁", count = 20)
                ),
                byWorkType = listOf(
                    WorkTypeCount(workType = "고정", count = 25),
                    WorkTypeCount(workType = "순환", count = 15),
                    WorkTypeCount(workType = "방문", count = 10)
                ),
                byChannelAndWorkType = listOf(
                    ChannelWorkTypeItem(
                        channelName = "대형마트",
                        fixed = 10,
                        alternating = 5,
                        visiting = 3
                    )
                ),
                previousMonth = PreviousMonthData(
                    byWorkType = listOf(
                        WorkTypeCount(workType = "고정", count = 24),
                        WorkTypeCount(workType = "순환", count = 14),
                        WorkTypeCount(workType = "방문", count = 12)
                    )
                )
            ),
            basicStats = BasicStats(
                branchName = branchName,
                staffType = StaffTypeCount(promotion = 35, osc = 15),
                totalByPosition = TotalByPosition(active = 45, onLeave = 5),
                byAgeGroup = listOf(
                    AgeGroupCount(ageGroup = "20대", count = 10),
                    AgeGroupCount(ageGroup = "30대", count = 20),
                    AgeGroupCount(ageGroup = "40대", count = 15),
                    AgeGroupCount(ageGroup = "50대 이상", count = 5)
                ),
                byWorkType = WorkTypeStats(fixed = 25, alternating = 15, visiting = 10)
            )
        )
    }

    @Nested
    @DisplayName("GET /api/v1/admin/dashboard - 대시보드 조회")
    inner class GetDashboard {

        @Test
        @DisplayName("성공 - 파라미터 없이 기본 대시보드 조회")
        fun getDashboard_success_noParams() {
            // Given
            val mockResponse = buildSampleDashboardResponse()
            whenever(adminDashboardService.getDashboard(any(), isNull(), isNull()))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("대시보드 조회 성공"))
                // salesSummary
                .andExpect(jsonPath("$.data.sales_summary.year_month").value("2026-03"))
                .andExpect(jsonPath("$.data.sales_summary.branch_name").value("서울지점"))
                .andExpect(jsonPath("$.data.sales_summary.target_amount").value(500_000_000))
                .andExpect(jsonPath("$.data.sales_summary.actual_amount").value(420_000_000))
                .andExpect(jsonPath("$.data.sales_summary.progress_rate").value(84.0))
                .andExpect(jsonPath("$.data.sales_summary.reference_progress_rate").value(80.0))
                .andExpect(jsonPath("$.data.sales_summary.last_year_amount").value(400_000_000))
                .andExpect(jsonPath("$.data.sales_summary.last_year_ratio").value(105.0))
                .andExpect(jsonPath("$.data.sales_summary.channel_sales[0].channel_name").value("대형마트"))
                .andExpect(jsonPath("$.data.sales_summary.channel_sales[0].target_amount").value(200_000_000))
                .andExpect(jsonPath("$.data.sales_summary.channel_sales[1].channel_name").value("편의점"))
                // staffDeployment
                .andExpect(jsonPath("$.data.staff_deployment.year_month").value("2026-03"))
                .andExpect(jsonPath("$.data.staff_deployment.branch_name").value("서울지점"))
                .andExpect(jsonPath("$.data.staff_deployment.by_account_type[0].account_type").value("직영"))
                .andExpect(jsonPath("$.data.staff_deployment.by_account_type[0].count").value(30))
                .andExpect(jsonPath("$.data.staff_deployment.by_work_type[0].work_type").value("고정"))
                .andExpect(jsonPath("$.data.staff_deployment.by_work_type[0].count").value(25))
                .andExpect(jsonPath("$.data.staff_deployment.by_channel_and_work_type[0].channel_name").value("대형마트"))
                .andExpect(jsonPath("$.data.staff_deployment.by_channel_and_work_type[0].fixed").value(10))
                .andExpect(jsonPath("$.data.staff_deployment.previous_month.by_work_type[0].work_type").value("고정"))
                // basicStats
                .andExpect(jsonPath("$.data.basic_stats.branch_name").value("서울지점"))
                .andExpect(jsonPath("$.data.basic_stats.staff_type.promotion").value(35))
                .andExpect(jsonPath("$.data.basic_stats.staff_type.osc").value(15))
                .andExpect(jsonPath("$.data.basic_stats.total_by_position.active").value(45))
                .andExpect(jsonPath("$.data.basic_stats.total_by_position.on_leave").value(5))
                .andExpect(jsonPath("$.data.basic_stats.by_age_group[0].age_group").value("20대"))
                .andExpect(jsonPath("$.data.basic_stats.by_age_group[0].count").value(10))
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.fixed").value(25))
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.alternating").value(15))
                .andExpect(jsonPath("$.data.basic_stats.by_work_type.visiting").value(10))
        }

        @Test
        @DisplayName("성공 - yearMonth 파라미터 지정 조회")
        fun getDashboard_success_withYearMonth() {
            // Given
            val mockResponse = buildSampleDashboardResponse(yearMonth = "2026-01")
            whenever(adminDashboardService.getDashboard(eq(1L), eq("2026-01"), isNull()))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-01")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sales_summary.year_month").value("2026-01"))
                .andExpect(jsonPath("$.data.staff_deployment.year_month").value("2026-01"))
        }

        @Test
        @DisplayName("성공 - yearMonth + branchCode 파라미터 지정 조회")
        fun getDashboard_success_withYearMonthAndBranchCode() {
            // Given
            val mockResponse = buildSampleDashboardResponse(yearMonth = "2026-02", branchName = "부산지점")
            whenever(adminDashboardService.getDashboard(eq(1L), eq("2026-02"), eq("B001")))
                .thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-02")
                    .param("branchCode", "B001")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sales_summary.year_month").value("2026-02"))
                .andExpect(jsonPath("$.data.sales_summary.branch_name").value("부산지점"))
        }

        @Test
        @DisplayName("실패 - 잘못된 yearMonth 형식 시 400 VALIDATION_ERROR")
        fun getDashboard_invalidYearMonthFormat() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "202603")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("실패 - yearMonth 월 범위 초과 시 400 VALIDATION_ERROR")
        fun getDashboard_invalidYearMonthRange() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-13")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("서비스 호출 시 올바른 인자가 전달되는지 검증")
        fun getDashboard_verifyServiceCalledWithCorrectArgs() {
            // Given
            val mockResponse = buildSampleDashboardResponse()
            whenever(adminDashboardService.getDashboard(eq(1L), eq("2026-03"), eq("S001")))
                .thenReturn(mockResponse)

            // When
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .param("yearMonth", "2026-03")
                    .param("branchCode", "S001")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)

            // Then
            verify(adminDashboardService).getDashboard(
                eq(1L),
                eq("2026-03"),
                eq("S001")
            )
        }

        @Test
        @DisplayName("서비스 호출 시 파라미터 없으면 null 전달 검증")
        fun getDashboard_verifyServiceCalledWithNulls() {
            // Given
            val mockResponse = buildSampleDashboardResponse()
            whenever(adminDashboardService.getDashboard(eq(1L), isNull(), isNull()))
                .thenReturn(mockResponse)

            // When
            mockMvc.perform(
                get("/api/v1/admin/dashboard")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)

            // Then
            verify(adminDashboardService).getDashboard(
                eq(1L),
                isNull(),
                isNull()
            )
        }
    }
}
