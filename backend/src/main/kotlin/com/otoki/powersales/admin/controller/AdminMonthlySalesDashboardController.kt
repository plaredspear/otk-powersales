package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardSummaryResponse
import com.otoki.powersales.domain.sales.service.MonthlySalesAdminQueryService
import com.otoki.powersales.domain.sales.service.MonthlySalesDashboardExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/sales/monthly")
@PermissionResource("monthly_sales_history")
class AdminMonthlySalesDashboardController(
    private val queryService: MonthlySalesAdminQueryService,
    private val excelExporter: MonthlySalesDashboardExcelExporter,
) {

    /** 상단 KPI + 최근 6개월 월별 추이. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/summary")
    fun getSummary(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) distributionKeyword: String?,
        @RequestParam(required = false) accountTypeKeyword: String?,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardSummaryResponse>> {
        val response = queryService.getSummary(
            scope, year, month, costCenterCodes, customerKeyword, accountGroup,
            distributionKeyword, accountTypeKeyword,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 명세 — 페이징 + 정렬 + 필터. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun getList(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionKeyword: String?,
        @RequestParam(required = false) accountTypeKeyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardListResponse>> {
        val request = MonthlySalesDashboardListRequest(
            year = year, month = month, costCenterCodes = costCenterCodes,
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionKeyword = distributionKeyword, accountTypeKeyword = accountTypeKeyword,
            page = page, size = size, sort = sort,
        )
        val response = queryService.getList(scope, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 거래처별 명세 엑셀 다운로드. 페이징 미적용 (권한 범위 전체 export). */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/list/export")
    fun exportList(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) accountGroup: String?,
        @RequestParam(required = false) customerKeyword: String?,
        @RequestParam(required = false) distributionKeyword: String?,
        @RequestParam(required = false) accountTypeKeyword: String?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<ByteArray> {
        val request = MonthlySalesDashboardListRequest(
            year = year, month = month, costCenterCodes = costCenterCodes,
            accountIds = accountIds ?: emptyList(),
            accountGroup = accountGroup, customerKeyword = customerKeyword,
            distributionKeyword = distributionKeyword, accountTypeKeyword = accountTypeKeyword,
            page = 0, size = Int.MAX_VALUE, sort = sort,
        )
        val items = queryService.getListForExport(scope, request)
        val excel = excelExporter.export(year, month, items)
        return ExcelResponseUtils.build(excel)
    }

    /** 단건 거래처 상세 — 모바일 동등 6 영역. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail/{customerId}")
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable customerId: Long,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<MonthlySalesDashboardDetailResponse>> {
        val response = queryService.getDetail(scope, customerId, year, month)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
