package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTConfirmedReportService
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTMasterService
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import io.mockk.every
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 전문행사조 지점 축 정합 테스트 — 셀렉터 옵션과 조회 스코프가 **동일 출처**
 * ([DashboardBranchResolver]) 인지 검증한다. 여사원 현황
 * ([AdminFemaleEmployeeControllerTest]) 과 같은 지점 목록을 쓰는 것이 요구사항이므로,
 * resolver 를 갈아끼우면 셀렉터와 조회가 함께 따라가야 한다.
 */
@WebMvcTest(AdminPPTMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPPTMasterController 지점 스코프 테스트")
class AdminPPTMasterControllerBranchScopeTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminPPTMasterService: AdminPPTMasterService

    @MockkBean
    private lateinit var pptConfirmedReportService: AdminPPTConfirmedReportService

    @MockkBean
    private lateinit var dashboardBranchResolver: DashboardBranchResolver

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        // 본 컨트롤러는 @CurrentDataScope 를 더 이상 쓰지 않지만 (지점 스코프는 resolveBranchScope 가 산출),
        // Spring 이 모든 핸들러 파라미터마다 supportsParameter 를 호출하므로 stub 을 유지한다.
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
    }

    private fun emptyMasterList() = PPTMasterListResponse(
        content = emptyList(), totalElements = 0, totalPages = 0, number = 0, size = 20,
    )

    @Test
    @DisplayName("GET /ppt-masters/branches - 셀렉터 옵션은 DashboardBranchResolver (여사원 현황과 동일 목록)")
    fun getBranches_usesDashboardResolver() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
            BranchResponse(branchCode = "5816", branchName = "강북4지점"),
        )

        mockMvc.perform(get("/api/v1/admin/ppt-masters/branches"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].branchCode").value("5815"))
            .andExpect(jsonPath("$.data[0].branchName").value("강북1지점"))
            .andExpect(jsonPath("$.data[1].branchCode").value("5816"))
    }

    @Test
    @DisplayName("전사 권한자 - 지점 미선택도 화이트리스트로 제한 (전건 조회 아님)")
    fun getMasters_allBranchesUserIsLimitedToWhitelist() {
        // 전사 권한자: DashboardBranchResolver 가 고정 화이트리스트를 내려준다 (여기선 2건으로 축약).
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
            BranchResponse(branchCode = "5816", branchName = "강북4지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.getMasters(
                capture(scopeSlot), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns emptyMasterList()

        mockMvc.perform(get("/api/v1/admin/ppt-masters"))
            .andExpect(status().isOk)

        // 전사 권한자여도 isAllBranches=false — 지점 미선택이면 화이트리스트 IN 조회 (기존 @CurrentDataScope 는 전건이었다).
        assertThat(scopeSlot.captured.isAllBranches).isFalse()
        assertThat(scopeSlot.captured.branchCodes).containsExactlyInAnyOrder("5815", "5816")
        assertThat(scopeSlot.captured.effectiveBranchCodes(null))
            .isEqualTo(EffectiveBranchResult.Filtered(listOf("5815", "5816")))
        // 화이트리스트 밖 지점(FS마케팅1팀 등 여사원 조직 밖 부서) 은 IDOR 판정에서 차단.
        assertThat(scopeSlot.captured.effectiveBranchCodes("9999"))
            .isEqualTo(EffectiveBranchResult.NoAccess)
    }

    @Test
    @DisplayName("셀렉터에 있는 지점 선택 - 그 지점으로 좁혀 조회 (셀렉터=조회 스코프 정합)")
    fun getMasters_selectedBranchWithinWhitelist() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
            BranchResponse(branchCode = "5816", branchName = "강북4지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.getMasters(
                capture(scopeSlot), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns emptyMasterList()

        mockMvc.perform(get("/api/v1/admin/ppt-masters").param("branchCode", "5816"))
            .andExpect(status().isOk)

        // 셀렉터에 노출된 지점은 NoAccess 가 아니라 그 지점으로 좁혀진다.
        assertThat(scopeSlot.captured.effectiveBranchCodes("5816"))
            .isEqualTo(EffectiveBranchResult.Filtered(listOf("5816")))
    }

    @Test
    @DisplayName("셀렉터 밖 지점 요청 - NoAccess 스코프로 빈 결과 (IDOR 차단)")
    fun getMasters_deniesBranchOutsideSelector() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.getMasters(
                capture(scopeSlot), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns emptyMasterList()

        mockMvc.perform(get("/api/v1/admin/ppt-masters").param("branchCode", "Z999"))
            .andExpect(status().isOk)

        // branchCodes 가 비어 어떤 요청이든 NoAccess — 서비스가 빈 목록으로 응답한다.
        assertThat(scopeSlot.captured.branchCodes).isEmpty()
        assertThat(scopeSlot.captured.effectiveBranchCodes("Z999"))
            .isEqualTo(EffectiveBranchResult.NoAccess)
    }

    @Test
    @DisplayName("확정인원 보고서 - 마스터 목록과 동일 지점 스코프 적용")
    fun getConfirmedReport_sharesBranchScope() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            pptConfirmedReportService.getReport(capture(scopeSlot), any())
        } returns PPTConfirmedReportResponse(emptyList())

        mockMvc.perform(get("/api/v1/admin/ppt-masters/confirmed-report"))
            .andExpect(status().isOk)

        assertThat(scopeSlot.captured.isAllBranches).isFalse()
        assertThat(scopeSlot.captured.branchCodes).containsExactly("5815")
    }

    @Test
    @DisplayName("엑셀 export - 목록과 동일 지점 스코프 적용 (대량 반출 경로 회귀 방지)")
    fun exportMasters_sharesBranchScope() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
            BranchResponse(branchCode = "5816", branchName = "강북4지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.exportToExcel(capture(scopeSlot), any(), any(), any(), any(), any(), any())
        } returns ExcelResult(ByteArray(0), "전문행사조.xlsx")

        mockMvc.perform(get("/api/v1/admin/ppt-masters/export"))
            .andExpect(status().isOk)

        // export 도 전건이 아니라 화이트리스트 IN — 목록에서 막힌 지점이 파일로 새지 않는다.
        assertThat(scopeSlot.captured.isAllBranches).isFalse()
        assertThat(scopeSlot.captured.branchCodes).containsExactlyInAnyOrder("5815", "5816")
    }

    @Test
    @DisplayName("이력 목록/엑셀 - 목록과 동일 지점 스코프 적용")
    fun historyEndpoints_shareBranchScope() {
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "5815", branchName = "강북1지점"),
        )
        val listScopeSlot = slot<DataScope>()
        val exportScopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.getAllHistory(
                capture(listScopeSlot), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns PPTMasterHistoryListResponse(
            content = emptyList(), totalElements = 0, totalPages = 0, number = 0, size = 20,
        )
        every {
            adminPPTMasterService.exportHistoryToExcel(
                capture(exportScopeSlot), any(), any(), any(), any(), any(), any(),
            )
        } returns ExcelResult(ByteArray(0), "전문행사조이력.xlsx")

        mockMvc.perform(get("/api/v1/admin/ppt-histories")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/admin/ppt-histories/export")).andExpect(status().isOk)

        assertThat(listScopeSlot.captured.branchCodes).containsExactly("5815")
        assertThat(exportScopeSlot.captured.branchCodes).containsExactly("5815")
    }

    @Test
    @DisplayName("지점 권한자 - 조직 트리 전체가 스코프 (여사원 현황과 동일 위임 경로)")
    fun getMasters_branchUserUsesOrgTree() {
        // 지점 권한자는 DashboardBranchResolver 가 WomenScheduleBranchResolver 로 위임 — 조직 트리 다건.
        every { dashboardBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "A001", branchName = "서울1지점"),
            BranchResponse(branchCode = "A002", branchName = "서울2지점"),
        )
        val scopeSlot = slot<DataScope>()
        every {
            adminPPTMasterService.getMasters(
                capture(scopeSlot), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns emptyMasterList()

        mockMvc.perform(get("/api/v1/admin/ppt-masters").param("branchCode", "A002"))
            .andExpect(status().isOk)

        // 본인 소속 1건이 아니라 트리 전체 — 형제 지점(A002) 선택이 NoAccess 가 되지 않는다.
        assertThat(scopeSlot.captured.branchCodes).containsExactlyInAnyOrder("A001", "A002")
        assertThat(scopeSlot.captured.effectiveBranchCodes("A002"))
            .isInstanceOf(EffectiveBranchResult.Filtered::class.java)
    }
}
