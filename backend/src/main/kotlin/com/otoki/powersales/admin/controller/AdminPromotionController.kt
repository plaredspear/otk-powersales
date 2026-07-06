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
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.ReportBranchScopeService
import com.otoki.powersales.domain.activity.promotion.service.AdminPromotionService
import com.otoki.powersales.domain.activity.promotion.service.AdminPromotionTargetActualReportService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
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
    private val reportBranchScopeService: ReportBranchScopeService,
) {

    /**
     * 행사사원 목표 대비 실적 보고서 화면 지점 셀렉터 옵션.
     *
     * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 화면 게이팅과 동일한 promotion READ 로 가드.
     */
    @GetMapping("/target-actual-report/branches")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getTargetActualReportBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(reportBranchScopeService.getBranches(principal)))
    }

    /**
     * 행사마스터 목록 화면 지점 셀렉터 옵션.
     *
     * 전사 권한자는 전 지점, 그 외는 본인 지점 1건 (목표 대비 실적 보고서 셀렉터와 동일 산출 —
     * [ReportBranchScopeService.getBranches]). 화면 게이팅과 동일한 promotion READ 로 가드.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPromotionBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(reportBranchScopeService.getBranches(principal)))
    }

    @GetMapping("/form-meta")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getPromotionFormMeta(): ResponseEntity<ApiResponse<PromotionFormMetaResponse>> {
        val response = adminPromotionService.getPromotionFormMeta()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 행사사원 목표 대비 실적 보고서 조회 (Spec #845). ScheduleDate 기간(필수) 조회.
     * 행사명 그룹 + 소계 + 전체 합계 + 도넛 차트 데이터. branchCode 선택 시 그 지점(여사원일정 소속)으로 좁힘.
     */
    @GetMapping("/target-actual-report")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getTargetActualReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ApiResponse<PromotionTargetActualReportResponse>> {
        val branchScope = reportBranchScopeService.effectiveBranchCodes(principal, branchCode)
        val response = targetActualReportService.getReport(startDate, endDate, branchScope)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 행사사원 목표 대비 실적 엑셀 다운로드. */
    @GetMapping("/target-actual-report/export")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun exportTargetActualReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ByteArray> {
        val branchScope = reportBranchScopeService.effectiveBranchCodes(principal, branchCode)
        val result = targetActualReportService.exportReport(startDate, endDate, branchScope)
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
        @RequestParam(required = false) branchCode: String?,
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
            ownerOnly = ownerOnly,
            branchCodes = resolveBranchCodes(principal, branchCode)
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
        @RequestParam(required = false) branchCode: String?,
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
            branchCodes = resolveBranchCodes(principal, branchCode),
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

    /**
     * 목록/엑셀 지점 필터에 넘길 지점 코드 목록 산출.
     *
     * [ReportBranchScopeService.effectiveBranchCodes] 결과를 목록 쿼리용 `List<String>?` 로 변환한다:
     * - All (전사 권한자 + 선택 없음) → null (지점 필터 미적용, 가시 범위 전건)
     * - Filtered → 해당 지점 코드 (그 지점으로 좁힘)
     * - NoAccess (권한 지점 없음 / 선택값이 본인 지점 밖) → emptyList (매칭 0건, IDOR 차단)
     */
    private fun resolveBranchCodes(principal: WebUserPrincipal, branchCode: String?): List<String>? {
        return when (val result = reportBranchScopeService.effectiveBranchCodes(principal, branchCode)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> emptyList()
        }
    }
}
