package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
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
    private lateinit var branchScopeGateway: BranchScopeGateway

    /**
     * 전산실적 / POS매출은 근무형태별 여사원인원현황과 동일하게 매출 계열 프로파일
     * ([BranchScopeProfile.SALES] — 전사 권한자 34개 화이트리스트) 로 위임한다.
     * 위임 경로가 같으므로 두 URL 을 함께 검증한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/{electronic,pos}/branches - 매출 계열 프로파일(SALES) 위임")
    fun salesDashboardBranches_delegatesToDashboardResolver() {
        every { branchScopeGateway.resolveBranches(any(), any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
        )

        for (path in listOf("electronic", "pos")) {
            mockMvc.perform(get("/api/v1/admin/sales/$path/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
                .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))
        }

        verify(exactly = 2) { branchScopeGateway.resolveBranches(any(), BranchScopeProfile.SALES) }
    }

    /**
     * 월 매출(물류배부) 도 [BranchScopeProfile.SALES] (전사 권한자 34개 화이트리스트) 로 위임한다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/monthly/branches - 매출 계열 프로파일(SALES) 위임")
    fun monthlyBranches_delegatesToDashboardResolver() {
        every { branchScopeGateway.resolveBranches(any(), any()) } returns listOf(
            BranchResponse(branchCode = "4889", branchName = "영업지원2팀"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/monthly/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("4889"))
            .andExpect(jsonPath("$.data[0].branchName").value("영업지원2팀"))

        verify(exactly = 1) { branchScopeGateway.resolveBranches(any(), BranchScopeProfile.SALES) }
    }

    /**
     * 월별 투입적합성은 근무형태별 여사원인원현황(`/team-schedule/branches`) 과 동일하게
     * [BranchScopeProfile.SALES] (전사 권한자 34개 화이트리스트) 로 위임한다 — 두 화면의 지점 셀렉터가
     * 같아야 한다는 운영 요구.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/input-adequacy/branches - 매출 계열 프로파일(SALES) 위임")
    fun inputAdequacyBranches_delegatesToDashboardResolver() {
        every { branchScopeGateway.resolveBranches(any(), any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/input-adequacy/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
            .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))

        verify(exactly = 1) { branchScopeGateway.resolveBranches(any(), BranchScopeProfile.SALES) }
    }

    /**
     * 배치 적합성만 행사마스터와 동일한 [BranchScopeProfile.MASTER_LIST] 로 위임한다 — 통합(UNIFIED)
     * 에서는 SALES 와 산출이 같지만, LEGACY(전환 이전) 에서는 본인 지점 1건이라 프로파일이 갈린다.
     */
    @Test
    @DisplayName("GET /api/v1/admin/sales/deployment/branches - 마스터 목록 프로파일(MASTER_LIST) 위임")
    fun deploymentBranches_delegatesToWhitelistResolver() {
        every { branchScopeGateway.resolveBranches(any(), any()) } returns listOf(
            BranchResponse(branchCode = "5817", branchName = "강남1지점"),
            BranchResponse(branchCode = "5834", branchName = "강릉지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/deployment/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("5817"))
            .andExpect(jsonPath("$.data[0].branchName").value("강남1지점"))
            .andExpect(jsonPath("$.data[1].branchName").value("강릉지점"))

        verify(exactly = 1) { branchScopeGateway.resolveBranches(any(), BranchScopeProfile.MASTER_LIST) }
        verify(exactly = 0) { branchScopeGateway.resolveBranches(any(), BranchScopeProfile.SALES) }
    }
}
