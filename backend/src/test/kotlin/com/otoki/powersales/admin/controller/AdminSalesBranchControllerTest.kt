package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.admin.service.WhitelistBranchScopeResolver
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminSalesBranchController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSalesBranchController 테스트")
class AdminSalesBranchControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var dashboardBranchResolver: DashboardBranchResolver

    @MockkBean
    private lateinit var whitelistBranchScopeResolver: WhitelistBranchScopeResolver

    /**
     * 전산실적 / POS매출은 근무형태별 여사원인원현황과 동일하게 [DashboardBranchResolver]
     * (전사 권한자 34개 화이트리스트) 로 위임한다 — 위임 경로가 같으므로 두 URL 을 함께 검증한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/{electronic,pos}/branches - 대시보드 화이트리스트(dashboardBranchResolver) 위임")
    fun salesDashboardBranches_delegatesToDashboardResolver() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
        )

        for (path in listOf("electronic", "pos")) {
            mockMvc.perform(get("/api/v1/admin/sales/$path/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
                .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))
        }

        verify(exactly = 2) { dashboardBranchResolver.resolveBranches(any()) }
    }

    /**
     * 월 매출(물류배부) 도 [DashboardBranchResolver] (전사 권한자 34개 화이트리스트) 로 위임한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/monthly/branches - 대시보드 화이트리스트(dashboardBranchResolver) 위임")
    fun monthlyBranches_delegatesToDashboardResolver() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "4889", branchName = "영업지원2팀"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/monthly/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("4889"))
            .andExpect(jsonPath("$.data[0].branchName").value("영업지원2팀"))

        verify(exactly = 1) { dashboardBranchResolver.resolveBranches(any()) }
    }

    /**
     * 월별 투입적합성은 근무형태별 여사원인원현황(`/team-schedule/branches`) 과 동일하게
     * [DashboardBranchResolver] (전사 권한자 34개 화이트리스트) 로 위임한다 — 두 화면의 지점 셀렉터가
     * 같아야 한다는 운영 요구. 조직 트리 리졸버를 타면 팀 단위 조직이 섞여 목록이 달라진다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/input-adequacy/branches - 대시보드 화이트리스트(dashboardBranchResolver) 위임")
    fun inputAdequacyBranches_delegatesToDashboardResolver() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/input-adequacy/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
            .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))

        verify(exactly = 1) { dashboardBranchResolver.resolveBranches(any()) }
    }

    /**
     * 배치 적합성만 고정 지점 화이트리스트([WhitelistBranchScopeResolver], 행사마스터와 동일) 로 위임한다.
     *
     * 나머지 화면이 쓰는 [DashboardBranchResolver] 는 비전사 사용자에게 조직 트리(형제 지점) 를 주지만,
     * 배치 적합성은 지점 단위 화면이라 본인 지점만 노출하는 이 경로를 유지한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/deployment/branches - 고정 지점 화이트리스트(whitelistBranchScopeResolver) 위임")
    fun deploymentBranches_delegatesToWhitelistResolver() {
        every { whitelistBranchScopeResolver.getBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
            BranchResponse(branchCode = "5834", branchName = "강릉지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/deployment/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
            .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))
            .andExpect(jsonPath("$.data[1].branchName").value("강릉지점"))

        verify(exactly = 1) { whitelistBranchScopeResolver.getBranches(any()) }
        verify(exactly = 0) { dashboardBranchResolver.resolveBranches(any()) }
    }
}
