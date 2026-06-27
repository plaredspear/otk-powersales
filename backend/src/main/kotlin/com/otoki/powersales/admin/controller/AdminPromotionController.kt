package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PromotionPosProductRequest
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionDetailResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionFormMetaResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionPosProductResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionTargetActualReportResponse
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.activity.promotion.service.AdminPromotionService
import com.otoki.powersales.domain.activity.promotion.service.AdminPromotionTargetActualReportService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/promotions")
@Validated
class AdminPromotionController(
    private val adminPromotionService: AdminPromotionService,
    private val targetActualReportService: AdminPromotionTargetActualReportService,
) {

    @GetMapping("/form-meta")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPromotionFormMeta(): ResponseEntity<ApiResponse<PromotionFormMetaResponse>> {
        val response = adminPromotionService.getPromotionFormMeta()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 행사사원 목표 대비 실적 보고서 조회 (Spec #845). ScheduleDate 기간(필수) 전사 조회.
     * 행사명 그룹 + 소계 + 전체 합계 + 도넛 차트 데이터.
     */
    @GetMapping("/target-actual-report")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getTargetActualReport(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate,
    ): ResponseEntity<ApiResponse<PromotionTargetActualReportResponse>> {
        val response = targetActualReportService.getReport(startDate, endDate)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 행사사원 목표 대비 실적 엑셀 다운로드. */
    @GetMapping("/target-actual-report/export")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun exportTargetActualReport(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate,
    ): ResponseEntity<ByteArray> {
        val result = targetActualReportService.exportReport(startDate, endDate)
        return ExcelResponseUtils.build(result)
    }

    /** 행사마스터 목록 엑셀 다운로드 — 목록과 동일한 가시 범위/필터로 전량 추출 (최대 건수 제한 적용). */
    @GetMapping("/export")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun exportPromotions(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) promotionType: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) accountName: String?,
        @RequestParam(required = false) @Size(min = 1, max = 40) accountNumber: String?,
        @RequestParam(required = false) @Size(min = 1, max = 255) category1: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) primaryProduct: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) employeeKeyword: String?,
        @RequestParam(required = false, defaultValue = "false") ownerOnly: Boolean,
    ): ResponseEntity<ByteArray> {
        val result = adminPromotionService.exportPromotions(
            scope = scope,
            keyword = keyword,
            promotionType = promotionType,
            startDate = startDate,
            endDate = endDate,
            accountName = accountName,
            accountNumber = accountNumber,
            category1 = category1,
            primaryProduct = primaryProduct,
            employeeKeyword = employeeKeyword,
            ownerOnly = ownerOnly
        )
        return ExcelResponseUtils.build(result)
    }

    @GetMapping
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPromotions(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) promotionType: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) accountName: String?,
        @RequestParam(required = false) @Size(min = 1, max = 40) accountNumber: String?,
        @RequestParam(required = false) @Size(min = 1, max = 255) category1: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) primaryProduct: String?,
        @RequestParam(required = false) @Size(min = 1, max = 100) employeeKeyword: String?,
        @RequestParam(required = false, defaultValue = "false") ownerOnly: Boolean,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<PromotionListResponse>> {
        val response = adminPromotionService.getPromotions(
            scope = scope,
            keyword = keyword,
            promotionType = promotionType,
            startDate = startDate,
            endDate = endDate,
            accountName = accountName,
            accountNumber = accountNumber,
            category1 = category1,
            primaryProduct = primaryProduct,
            employeeKeyword = employeeKeyword,
            ownerOnly = ownerOnly,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.getPromotion(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    // SF Promotion 상세의 "상세 POS품목" Related List 동등.
    @GetMapping("/{id}/pos-products")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPosProducts(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<List<PromotionPosProductResponse>>> {
        val response = adminPromotionService.getPosProducts(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    // SF Promotion 상세의 "새 상세 POS품목" 다이얼로그 동등.
    @PostMapping("/{id}/pos-products")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun createPosProduct(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionPosProductRequest,
    ): ResponseEntity<ApiResponse<PromotionPosProductResponse>> {
        val response = adminPromotionService.createPosProduct(scope, id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun createPromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.createPromotion(principal.requireEmployeeId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun updatePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.updatePromotion(scope, principal, id, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun deletePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionService.deletePromotion(scope, principal, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    // UC-11: 행사마스터 복제 (폼 방식)
    // 레거시 PromotionCloneComponent Quick Action 동등.
    @PostMapping("/{id}/clone")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun clonePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.clonePromotion(scope, id, principal.requireEmployeeId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    // UC-12: 행사마스터 자식 포함 복제 (1클릭)
    // 레거시 ClonePromotionWithChildsController Quick Action 동등. body 없음.
    @PostMapping("/{id}/clone-with-children")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun cloneWithChildren(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.cloneWithChildren(scope, id, principal.requireEmployeeId())
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }
}
