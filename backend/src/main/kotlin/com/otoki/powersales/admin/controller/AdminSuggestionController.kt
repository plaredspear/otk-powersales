package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.ReportBranchScopeService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionCreateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionDetailResponse
import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionListResponse
import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionUpdateRequest
import com.otoki.powersales.domain.activity.suggestion.dto.response.LogisticsClaimReportResponse
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionAttachment
import com.otoki.powersales.domain.activity.suggestion.dto.response.SuggestionCreateResponse
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.otoki.powersales.domain.activity.suggestion.service.AdminLogisticsClaimReportService
import com.otoki.powersales.domain.activity.suggestion.service.AdminSuggestionFilterParams
import com.otoki.powersales.domain.activity.suggestion.service.AdminSuggestionService
import com.otoki.powersales.domain.activity.suggestion.service.LogisticsClaimReportPeriod
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
import java.time.LocalDate

/**
 * admin 제안 API (Spec #830 P1-B).
 *
 * 7 endpoint — 목록 / 상세 / 등록 / 수정 / 삭제 + 사진 추가 업로드 / 사진 단건 삭제.
 * SF Permission `suggestion` entity 의 READ / CREATE / UPDATE / DELETE operation 기반 권한 평가.
 */
@RestController
@RequestMapping("/api/v1/admin/suggestions")
class AdminSuggestionController(
    private val adminSuggestionService: AdminSuggestionService,
    private val logisticsClaimReportService: AdminLogisticsClaimReportService,
    private val reportBranchScopeService: ReportBranchScopeService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun getSuggestions(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) category: SuggestionCategory?,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) accountCode: String?,
        @RequestParam(required = false) actionStatus: SuggestionActionStatus?,
        @RequestParam(required = false) productCode: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminSuggestionListResponse>> {
        val response = adminSuggestionService.search(
            scope = scope,
            startDate = startDate,
            endDate = endDate,
            filter = AdminSuggestionFilterParams(
                category = category,
                employeeName = employeeName,
                accountCode = accountCode,
                actionStatus = actionStatus,
                productCode = productCode
            ),
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun getSuggestionDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<AdminSuggestionDetailResponse>> {
        val response = adminSuggestionService.getDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping(consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.CREATE)
    fun createSuggestion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestPart("request") request: AdminSuggestionCreateRequest,
        @RequestPart(required = false) photos: List<MultipartFile>?
    ): ResponseEntity<ApiResponse<SuggestionCreateResponse>> {
        val result = adminSuggestionService.create(
            adminEmployeeId = principal.userId,
            request = request,
            photos = photos
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "제안이 등록되었습니다"))
    }

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.EDIT)
    fun updateSuggestion(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminSuggestionUpdateRequest
    ): ResponseEntity<ApiResponse<AdminSuggestionDetailResponse>> {
        val response = adminSuggestionService.update(scope, id, request)
        return ResponseEntity.ok(ApiResponse.success(response, "제안이 수정되었습니다"))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.DELETE)
    fun deleteSuggestion(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        adminSuggestionService.softDelete(scope, id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "제안이 삭제되었습니다"))
    }

    @PostMapping("/{id}/photos", consumes = ["multipart/form-data"])
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.EDIT)
    fun uploadPhotos(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @RequestPart photos: List<MultipartFile>
    ): ResponseEntity<ApiResponse<List<SuggestionAttachment>>> {
        val result = adminSuggestionService.uploadPhotos(scope, id, photos)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "사진이 추가되었습니다"))
    }

    @DeleteMapping("/{suggestionId}/photos/{photoId}")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.EDIT)
    fun deletePhoto(
        @CurrentDataScope scope: DataScope,
        @PathVariable suggestionId: Long,
        @PathVariable photoId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        adminSuggestionService.deletePhoto(scope, suggestionId, photoId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "사진이 삭제되었습니다"))
    }

    /**
     * 물류 클레임 보고서 화면 지점 셀렉터 옵션.
     *
     * 전사 권한자는 전 지점, 그 외는 본인 지점 1건. 화면 게이팅과 동일한 suggestion READ 로 가드.
     */
    @GetMapping("/logistics-claim-report/branches")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun getLogisticsClaimReportBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(reportBranchScopeService.getBranches(principal)))
    }

    /**
     * (영업본부) 물류 클레임 보고서 조회 (Spec #844). period=THIS_MONTH/LAST_MONTH/CUSTOM.
     * CUSTOM 이면 startDate/endDate 필수. branchCode 선택 시 그 지점(등록 사원 소속)으로 좁힘.
     */
    @GetMapping("/logistics-claim-report")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun getLogisticsClaimReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false, defaultValue = "THIS_MONTH") period: LogisticsClaimReportPeriod,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ApiResponse<LogisticsClaimReportResponse>> {
        val branchScope = reportBranchScopeService.effectiveBranchCodes(principal, branchCode)
        val response = logisticsClaimReportService.getReport(period, startDate, endDate, branchScope)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 물류 클레임 보고서 엑셀 다운로드. */
    @GetMapping("/logistics-claim-report/export")
    @RequiresSfPermission(entity = "suggestion", operation = SfPermissionOperation.READ)
    fun exportLogisticsClaimReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false, defaultValue = "THIS_MONTH") period: LogisticsClaimReportPeriod,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ByteArray> {
        val branchScope = reportBranchScopeService.effectiveBranchCodes(principal, branchCode)
        val result = logisticsClaimReportService.exportReport(period, startDate, endDate, branchScope)
        return ExcelResponseUtils.build(result)
    }
}
