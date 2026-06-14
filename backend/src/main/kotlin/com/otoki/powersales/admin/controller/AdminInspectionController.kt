package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminCreateSiteActivityRequest
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminSiteActivityDetailResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminSiteActivityListResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminSiteActivityMutationResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminUpdateSiteActivityRequest
import com.otoki.powersales.domain.activity.inspection.enums.InspectionCategory
import com.otoki.powersales.domain.activity.inspection.enums.InspectionFieldType
import com.otoki.powersales.domain.activity.inspection.service.AdminSiteActivityMutationService
import com.otoki.powersales.domain.activity.inspection.service.AdminSiteActivityService
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * admin 현장점검 조회 API.
 *
 * 목록 / 상세 (조회) + 등록. SF Permission `site_activity` READ/CREATE 권한 평가 + 조회는 SharingRule
 * 데이터 가시 범위 적용. 레거시 SF `DKRetail__SiteAcitivity__c` 표준 페이지(목록/상세/New) 동등.
 */
@RestController
@RequestMapping("/api/v1/admin/inspections")
class AdminInspectionController(
    private val adminSiteActivityService: AdminSiteActivityService,
    private val adminSiteActivityMutationService: AdminSiteActivityMutationService,
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

    /**
     * 현장점검 결과 등록 — SF `DKRetail__SiteAcitivity__c` New 폼 동등.
     *
     * 관리자가 점검 사원(employeeId)을 지정해 결과를 수동 등록/보정. category=OWN 이면 productCode 사용,
     * COMPETITOR 면 경쟁사 필드 사용. photos 는 최대 10건.
     */
    @PostMapping(consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "site_activity", operation = SfPermissionOperation.CREATE)
    fun createInspection(
        @RequestPart("request") request: AdminCreateSiteActivityRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<AdminSiteActivityMutationResponse>> {
        val response = adminSiteActivityMutationService.create(request, photos)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    /** 현장점검 결과 수정 — SF 표준 Edit 폼 동등 (본문 필드 + lookup 재설정). */
    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "site_activity", operation = SfPermissionOperation.EDIT)
    fun updateInspection(
        @PathVariable id: Long,
        @RequestBody request: AdminUpdateSiteActivityRequest
    ): ResponseEntity<ApiResponse<AdminSiteActivityMutationResponse>> {
        val response = adminSiteActivityMutationService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 현장점검 결과 삭제 — soft delete. */
    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "site_activity", operation = SfPermissionOperation.DELETE)
    fun deleteInspection(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminSiteActivityMutationService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "현장점검이 삭제되었습니다"))
    }
}
