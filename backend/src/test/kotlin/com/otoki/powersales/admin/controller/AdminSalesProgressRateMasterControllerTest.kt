package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterListResponse
import com.otoki.powersales.domain.sales.service.AdminSalesProgressRateMasterService
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminSalesProgressRateMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSalesProgressRateMasterController 테스트")
class AdminSalesProgressRateMasterControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var service: AdminSalesProgressRateMasterService
    @MockkBean private lateinit var womenScheduleBranchResolver: WomenScheduleBranchResolver

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every {
            currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any())
        } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sales-progress-rate-masters/branches - 지점 셀렉터 옵션")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 권한별 지점 화이트리스트 반환")
        fun getBranches_success() {
            every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
                BranchResponse(branchCode = "1100", branchName = "강남지점"),
                BranchResponse(branchCode = "1200", branchName = "서초지점"),
            )

            mockMvc.perform(get("/api/v1/admin/sales-progress-rate-masters/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("1100"))
                .andExpect(jsonPath("$.data[0].branchName").value("강남지점"))
                .andExpect(jsonPath("$.data[1].branchCode").value("1200"))
        }

        @Test
        @DisplayName("라우팅 - /branches 리터럴 경로가 /{id}(Long) 보다 우선 매칭")
        fun getBranches_literalPathTakesPriorityOverIdPathVariable() {
            // /branches 가 @GetMapping("/{id}") 로 잘못 라우팅되면 "branches" → Long 변환 실패로 400 이 된다.
            // 리터럴 경로 우선 매칭이 보장되어 200 + 지점 목록이 반환되는지 검증 (회귀 가드).
            every { womenScheduleBranchResolver.resolveBranches(any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/admin/sales-progress-rate-masters/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/sales-progress-rate-masters - 목록 조회")
    inner class GetList {

        private val emptyResponse = SalesProgressRateMasterListResponse(
            content = emptyList(),
            page = 0,
            size = 20,
            totalElements = 0,
            totalPages = 0,
        )

        @Test
        @DisplayName("성공 - branchCode 쿼리 파라미터가 service 에 그대로 전달된다")
        fun getList_passesBranchCode() {
            every {
                service.getList(any(), any(), any(), any(), any(), any(), any())
            } returns emptyResponse

            mockMvc.perform(
                get("/api/v1/admin/sales-progress-rate-masters")
                    .param("branchCode", "1100")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify {
                service.getList(any(), null, null, null, "1100", 0, 20)
            }
        }

        @Test
        @DisplayName("성공 - branchCode 미전달 시 null 로 위임 (지점 필터 없음)")
        fun getList_withoutBranchCode() {
            every {
                service.getList(any(), any(), any(), any(), any(), any(), any())
            } returns emptyResponse

            mockMvc.perform(get("/api/v1/admin/sales-progress-rate-masters"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify {
                service.getList(any(), null, null, null, null, 0, 20)
            }
        }
    }
}
