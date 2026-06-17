package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyInputAdequacyResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyInputAdequacyService
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/monthly-input-adequacy")
class AdminMonthlyInputAdequacyController(
    private val adminMonthlyInputAdequacyService: AdminMonthlyInputAdequacyService
) {

    /** 1~12월 적합성 매트릭스 조회. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping
    fun getMatrix(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) workingCategory3: String?
    ): ResponseEntity<ApiResponse<MonthlyInputAdequacyResponse>> {
        val response = adminMonthlyInputAdequacyService.getMatrix(scope, year, costCenterCodes, workingCategory3)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 매트릭스 엑셀 다운로드. */
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    @GetMapping("/export")
    fun exportMatrix(
        @CurrentDataScope scope: DataScope,
        @RequestParam year: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) workingCategory3: String?
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyInputAdequacyService.exportMatrix(scope, year, costCenterCodes, workingCategory3)
        return ExcelResponseUtils.build(result)
    }
}
