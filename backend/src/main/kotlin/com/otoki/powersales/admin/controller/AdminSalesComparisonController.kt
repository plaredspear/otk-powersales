package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonMiddleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonSummaryResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SearchAccountCategoryItem
import com.otoki.powersales.domain.activity.schedule.service.AdminSalesComparisonService
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/sales-comparison")
class AdminSalesComparisonController(
    private val adminSalesComparisonService: AdminSalesComparisonService,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /** 거래처유형 picklist — `AccountCategoryMaster.useSearch=true` 항목 목록. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/categories")
    fun getSearchCategories(): ResponseEntity<ApiResponse<List<SearchAccountCategoryItem>>> {
        val response = adminSalesComparisonService.getSearchCategories()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 집계 모드 — 배치적합성 × 거래처카테고리 거래처 수 집계표. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) suitabilities: List<String>?,
        @RequestParam(required = false) categoryCodes: List<String>?,
        @RequestParam(required = false) workingCategory3: List<String>?
    ): ResponseEntity<ApiResponse<SalesComparisonSummaryResponse>> {
        val response = adminSalesComparisonService.getSummary(
            scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes),
            toSummaryFilter(suitabilities, categoryCodes, workingCategory3)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 중간집계 모드 — 거래처별 행 + 적합성별 소계 + 총계. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/middle")
    fun getMiddle(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?
    ): ResponseEntity<ApiResponse<SalesComparisonMiddleResponse>> {
        val response = adminSalesComparisonService.getMiddle(scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes), accountIds ?: emptyList())
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 상세 모드 — 사원별 행 + 총계. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail")
    fun getDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) workingCategory1: List<String>?,
        @RequestParam(required = false) workingCategory5: List<String>?
    ): ResponseEntity<ApiResponse<SalesComparisonDetailResponse>> {
        val response = adminSalesComparisonService.getDetail(
            scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes),
            accountIds ?: emptyList(),
            workingCategory1,
            workingCategory5
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 집계표 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/summary/export")
    fun exportSummary(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) suitabilities: List<String>?,
        @RequestParam(required = false) categoryCodes: List<String>?,
        @RequestParam(required = false) workingCategory3: List<String>?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportSummary(
            scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes),
            toSummaryFilter(suitabilities, categoryCodes, workingCategory3)
        )
    )

    /** 중간집계 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/middle/export")
    fun exportMiddle(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportMiddle(scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes), accountIds ?: emptyList())
    )

    /** 상세 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail/export")
    fun exportDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) workingCategory1: List<String>?,
        @RequestParam(required = false) workingCategory5: List<String>?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportDetail(
            scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes),
            accountIds ?: emptyList(),
            workingCategory1,
            workingCategory5
        )
    )

    /** 요청 파라미터(null/빈 리스트) → [AdminSalesComparisonService.SummaryFilter] 변환. null·빈값은 무필터(빈 set). */
    private fun toSummaryFilter(
        suitabilities: List<String>?,
        categoryCodes: List<String>?,
        workingCategory3: List<String>?
    ): AdminSalesComparisonService.SummaryFilter = AdminSalesComparisonService.SummaryFilter(
        suitabilities = suitabilities?.filter { it.isNotBlank() }?.toSet().orEmpty(),
        categoryCodes = categoryCodes?.filter { it.isNotBlank() }?.toSet().orEmpty(),
        workingCategory3 = workingCategory3?.filter { it.isNotBlank() }?.toSet().orEmpty()
    )

    /**
     * 지점 축 보정 — 셀렉터([AdminSalesBranchController]) 와 같은 출처로 조회 범위를 맞춘다.
     *
     * `DataScope` 의 지점 축은 비전사 사용자에게 본인 costCenterCode 1건이라, 셀렉터가 보여준 조직 트리의
     * 하위 지점을 골라도 교집합에서 탈락해 0건이 됐다. [BranchScopeGateway.applyDataScope] 가 그 축을
     * 셀렉터와 같은 조직 트리로 넓힌다(넓히기만 하므로 보이던 행이 사라지지 않는다).
     */
    private fun scopeOf(principal: WebUserPrincipal, scope: DataScope): DataScope =
        branchScopeGateway.applyDataScope(principal, scope)

    /**
     * 화면에서 고른 지점 코드 → 조회 코드 — 셀렉터 화이트리스트로 판정한 뒤 `BranchMapping` 으로 확장한다.
     * 권한 밖 지점을 요청하면 매칭 0건. 신/구 방식은 개발자 도구 토글로 전환한다(비교 검증용 한시 조치).
     */
    private fun codesOf(principal: WebUserPrincipal, costCenterCodes: List<String>): List<String> =
        branchScopeGateway.resolveQueryCodes(principal, costCenterCodes, BranchScopeProfile.SALES)

}
