package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
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

    @Test
    @DisplayName("GET /api/v1/admin/sales/branches - 권한별 지점 화이트리스트 반환 (monthly_sales_history 가드)")
    fun getBranches_returnsWhitelist() {
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
            BranchResponse(branchCode = "A002", branchName = "서울2지점"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("A001"))
            .andExpect(jsonPath("$.data[0].branchName").value("서울1지점"))
            .andExpect(jsonPath("$.data[1].branchCode").value("A002"))

        verify(exactly = 1) { womenScheduleBranchResolver.resolveBranches(any()) }
    }
}
