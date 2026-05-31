package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityDetailResponse
import com.otoki.powersales.inspection.dto.admin.AdminSiteActivityListResponse
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType
import com.otoki.powersales.inspection.service.AdminSiteActivityService
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * admin 현장점검 조회 API.
 *
 * 2 endpoint — 목록 / 상세 (조회 전용). SF Permission `site_activity` entity READ 기반 권한 평가 +
 * SharingRule 데이터 가시 범위 적용. 레거시 SF `현장점검(등록)` Theme 페이지의 SiteActivity 관리 관점.
 */
@RestController
@RequestMapping("/api/v1/admin/inspections")
class AdminInspectionController(
    private val adminSiteActivityService: AdminSiteActivityService
) {

    @GetMapping
    @RequiresSfPermission(entity = "site_activity", operation = SfPermissionOperation.READ)
    fun getInspections(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) category: InspectionCategory?,
        @RequestParam(required = false) fieldType: InspectionFieldType?,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) accountCode: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminSiteActivityListResponse>> {
        val response = adminSiteActivityService.search(
            scope = scope,
            startDate = startDate,
            endDate = endDate,
            category = category,
            fieldType = fieldType,
            employeeName = employeeName,
            accountCode = accountCode,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "site_activity", operation = SfPermissionOperation.READ)
    fun getInspectionDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<AdminSiteActivityDetailResponse>> {
        val response = adminSiteActivityService.getDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
