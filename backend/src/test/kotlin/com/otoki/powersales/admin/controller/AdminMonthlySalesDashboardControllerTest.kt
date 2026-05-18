package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardDetailResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardListItem
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardListResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardSummaryResponse
import com.otoki.powersales.sales.service.MonthlySalesAdminQueryService
import com.otoki.powersales.sales.service.MonthlySalesDashboardExcelExporter
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.user.entity.ProfileType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminMonthlySalesDashboardController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminMonthlySalesDashboardController 테스트")
class AdminMonthlySalesDashboardControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockitoBean private lateinit var queryService: MonthlySalesAdminQueryService
    @MockitoBean private lateinit var excelExporter: MonthlySalesDashboardExcelExporter
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

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
            active = true,
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        whenever(currentAdminContextArgumentResolver.supportsParameter(any())).thenAnswer { invocation ->
            val parameter = invocation.arguments[0] as MethodParameter
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        whenever(currentAdminContextArgumentResolver.resolveArgument(any(), anyOrNull(), any(), anyOrNull()))
            .thenReturn(DataScope(branchCodes = emptyList(), isAllBranches = true))
    }

    @Test
    @DisplayName("GET /summary - happy path")
    fun summaryHappyPath() {
        val response = MonthlySalesDashboardSummaryResponse(
            salesYear = 2026, salesMonth = 5,
            totalTargetAmount = 1_500_000L, totalAchievedAmount = 1_200_000L,
            overallAchievementRate = 80.0, referenceAchievementRate = 50.0,
            totalLastYearAchievedAmount = 1_000_000L, lastYearComparisonRatio = 120.0,
            monthlyTrend = emptyList(),
        )
        whenever(queryService.getSummary(any(), any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/admin/sales/monthly/summary")
                .param("year", "2026")
                .param("month", "5")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalTargetAmount").value(1500000))
            .andExpect(jsonPath("$.data.totalAchievedAmount").value(1200000))
            .andExpect(jsonPath("$.data.overallAchievementRate").value(80.0))
    }

    @Test
    @DisplayName("GET /list - 페이징 응답 반환")
    fun listHappyPath() {
        val response = MonthlySalesDashboardListResponse(
            items = listOf(
                MonthlySalesDashboardListItem(
                    accountId = 1, accountSfid = "ACC1", accountName = "거래처A",
                    sapAccountCode = "SAP1", branchCode = "1000", branchName = "서울지점",
                    salesYear = 2026, salesMonth = 5,
                    targetAmount = 1_000_000L, totalAchievedAmount = 800_000L, achievementRate = 80.0,
                    ambientAchievedAmount = 200_000L, noodleAchievedAmount = 200_000L,
                    frozenRefrigeratedAchievedAmount = 200_000L, oilFatAchievedAmount = 200_000L,
                    lastYearAchievedAmount = 700_000L, lastYearComparisonRatio = 114.3,
                    isConfirmed = true,
                )
            ),
            pageInfo = MonthlySalesDashboardListResponse.PageInfo(0, 20, 1L, 1),
        )
        whenever(queryService.getList(any(), any())).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/admin/sales/monthly/list")
                .param("year", "2026")
                .param("month", "5")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].accountId").value(1))
            .andExpect(jsonPath("$.data.items[0].accountName").value("거래처A"))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
    }

    @Test
    @DisplayName("GET /list/export - 엑셀 헤더 Content-Disposition")
    fun listExport() {
        whenever(queryService.getListForExport(any(), any())).thenReturn(emptyList())
        whenever(excelExporter.export(any(), any(), any())).thenReturn(
            MonthlySalesDashboardExcelExporter.ExcelResult(
                bytes = byteArrayOf(1, 2, 3),
                filename = "monthly-sales-2026-05.xlsx",
            )
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/monthly/list/export")
                .param("year", "2026")
                .param("month", "5")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("monthly-sales-2026-05.xlsx")))
    }

    @Test
    @DisplayName("GET /detail/{customerId} - 단건 상세 응답")
    fun detailHappyPath() {
        val response = MonthlySalesDashboardDetailResponse(
            customerId = 1, customerName = "거래처A",
            salesYear = 2026, salesMonth = 5,
            targetAmount = 1_000_000L, achievedAmount = 800_000L, achievementRate = 80.0,
            referenceAchievementRate = 50.0,
            categorySales = emptyList(),
            yearComparison = MonthlySalesDashboardDetailResponse.YearComparisonInfo(0, 0),
            monthlyAverage = MonthlySalesDashboardDetailResponse.MonthlyAverageInfo(0, 0, 1, 5),
        )
        whenever(queryService.getDetail(any(), any(), any(), any())).thenReturn(response)

        mockMvc.perform(
            get("/api/v1/admin/sales/monthly/detail/1")
                .param("year", "2026")
                .param("month", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.customerName").value("거래처A"))
            .andExpect(jsonPath("$.data.achievementRate").value(80.0))
    }
}
