package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
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
    private lateinit var womenScheduleBranchResolver: WomenScheduleBranchResolver

    @MockkBean
    private lateinit var dashboardBranchResolver: DashboardBranchResolver

    /**
     * 전산실적/POS/투입적합성/배치 4개 화면은 동일하게 [WomenScheduleBranchResolver] (조직 트리 스코프) 로
     * 위임한다 — URL 만 다르므로 대표 endpoint 1개로 위임 경로를 검증한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/electronic/branches - 조직 트리 스코프(womenScheduleBranchResolver) 위임")
    fun electronicBranches_delegatesToWomenScheduleResolver() {
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
            BranchResponse(branchCode = "A002", branchName = "서울2지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/electronic/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("A001"))
            .andExpect(jsonPath("$.data[0].branchName").value("서울1지점"))
            .andExpect(jsonPath("$.data[1].branchCode").value("A002"))

        verify(exactly = 1) { womenScheduleBranchResolver.resolveBranches(any()) }
        verify(exactly = 0) { dashboardBranchResolver.resolveBranches(any()) }
    }

    /**
     * 월 매출(물류배부) 만 대시보드와 동일한 [DashboardBranchResolver] (전사 권한자 34개 화이트리스트) 로
     * 위임한다 — 다른 4개와 다른 코드 경로이므로 별도 검증한다.
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
        verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
    }
}
