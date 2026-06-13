package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterDetailResponse
import com.otoki.powersales.domain.sales.dto.response.SalesProgressRateMasterListResponse
import com.otoki.powersales.domain.sales.service.AdminSalesProgressRateMasterService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 거래처목표등록마스터(SF `SalesProgressRateMaster__c`) admin 조회 API (읽기 전용).
 *
 * SF ListView "모두" 동등 목록 + 행 클릭 상세. 데이터 권위는 SF — 등록/수정/삭제 없음.
 * 권한 자원 = entity table name `sales_progress_rate_master` (EntitySfNameRegistry 자동 등록).
 */
@RestController
@RequestMapping("/api/v1/admin/sales-progress-rate-masters")
@Validated
class AdminSalesProgressRateMasterController(
    private val service: AdminSalesProgressRateMasterService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getList(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) targetYear: String?,
        @RequestParam(required = false) targetMonth: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<ApiResponse<SalesProgressRateMasterListResponse>> {
        val response = service.getList(scope, keyword, targetYear, targetMonth, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "sales_progress_rate_master", operation = SfPermissionOperation.READ)
    fun getDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<SalesProgressRateMasterDetailResponse>> {
        val response = service.getDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
