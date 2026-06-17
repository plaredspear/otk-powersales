package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonMiddleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SalesComparisonSummaryResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.SearchAccountCategoryItem
import com.otoki.powersales.domain.activity.schedule.service.AdminSalesComparisonService
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/sales-comparison")
class AdminSalesComparisonController(
    private val adminSalesComparisonService: AdminSalesComparisonService
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
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) suitabilities: List<String>?,
        @RequestParam(required = false) categoryCodes: List<String>?,
        @RequestParam(required = false) workingCategory3: List<String>?
    ): ResponseEntity<ApiResponse<SalesComparisonSummaryResponse>> {
        val response = adminSalesComparisonService.getSummary(
            scope, year, month, costCenterCodes,
            toSummaryFilter(suitabilities, categoryCodes, workingCategory3)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 중간집계 모드 — 거래처별 행 + 적합성별 소계 + 총계. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/middle")
    fun getMiddle(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?
    ): ResponseEntity<ApiResponse<SalesComparisonMiddleResponse>> {
        val response = adminSalesComparisonService.getMiddle(scope, year, month, costCenterCodes, accountIds ?: emptyList())
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 상세 모드 — 사원별 행 + 총계. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail")
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) workingCategory1: String?,
        @RequestParam(required = false) workingCategory5: String?
    ): ResponseEntity<ApiResponse<SalesComparisonDetailResponse>> {
        val response = adminSalesComparisonService.getDetail(
            scope, year, month, costCenterCodes,
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
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) suitabilities: List<String>?,
        @RequestParam(required = false) categoryCodes: List<String>?,
        @RequestParam(required = false) workingCategory3: List<String>?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportSummary(
            scope, year, month, costCenterCodes,
            toSummaryFilter(suitabilities, categoryCodes, workingCategory3)
        )
    )

    /** 중간집계 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/middle/export")
    fun exportMiddle(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportMiddle(scope, year, month, costCenterCodes, accountIds ?: emptyList())
    )

    /** 상세 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/detail/export")
    fun exportDetail(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) workingCategory1: String?,
        @RequestParam(required = false) workingCategory5: String?
    ): ResponseEntity<ByteArray> = ExcelResponseUtils.build(
        adminSalesComparisonService.exportDetail(
            scope, year, month, costCenterCodes,
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
}
