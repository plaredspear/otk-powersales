package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.service.BranchScopeProfile
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardSummaryResponse
import com.otoki.powersales.domain.sales.service.MonthlySalesAdminQueryService
import com.otoki.powersales.domain.sales.service.MonthlySalesDashboardExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 「월 매출(물류배부)」 web admin 대시보드.
 *
 * 권한 가드는 매출/실적 대시보드 3화면 공용 가상 자원 [SALES_DASHBOARD_RESOURCE] / READ —
 * 분리 배경과 부여 경로는 그 상수의 KDoc 참조. 적재 테이블 entity(`monthly_sales_history`) 가
 * 아니므로 「기준정보 > ORORA 월매출」 권한과는 독립이다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales/monthly")
@PermissionResource(SALES_DASHBOARD_RESOURCE)
class AdminMonthlySalesDashboardController(
    private val queryService: MonthlySalesAdminQueryService,
    private val excelExporter: MonthlySalesDashboardExcelExporter,
    private val branchScopeGateway: BranchScopeGateway,
) {

    /** 상단 KPI + 최근 6개월 월별 추이. */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) targetRegistration: String?,
        @RequestParam(required = false) deploymentFilter: String?,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardSummaryResponse>> {
        val response = queryService.getSummary(
            scopeOf(principal, scope), year, month, codesOf(principal, costCenterCodes), customerKeyword, accountGroup,
            distributionChannels ?: emptyList(), accountTypes ?: emptyList(),
            targetRegistration, deploymentFilter,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 명세 — 페이징 + 정렬 + 필터. */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) targetRegistration: String?,
        @RequestParam(required = false) deploymentFilter: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardListResponse>> {
        val request = MonthlySalesDashboardListRequest(
            year = year, month = month, costCenterCodes = codesOf(principal, costCenterCodes),
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            targetRegistration = targetRegistration, deploymentFilter = deploymentFilter,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scopeOf(principal, scope), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 명세 엑셀 다운로드. 페이징 미적용 (권한 범위 전체 export). */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionChannels: List<String>?,
        @RequestParam(required = false) accountTypes: List<String>?,
        @RequestParam(required = false) targetRegistration: String?,
        @RequestParam(required = false) deploymentFilter: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = MonthlySalesDashboardListRequest(
            year = year, month = month, costCenterCodes = codesOf(principal, costCenterCodes),
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionChannels = distributionChannels ?: emptyList(),
            accountTypes = accountTypes ?: emptyList(),
            targetRegistration = targetRegistration, deploymentFilter = deploymentFilter,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scopeOf(principal, scope), request)
        val excel = excelExporter.export(year, month, items)
        return ExcelResponseUtils.build(excel)
    }

    /** 단건 거래처 상세 — 모바일 동등 6 영역. */
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    @GetMapping("/detail/{customerId}")
    fun getDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable customerId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardDetailResponse>> {
        val response = queryService.getDetail(scopeOf(principal, scope), customerId, year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

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
