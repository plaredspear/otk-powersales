package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.ConfirmByIdsResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterResponse
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterConfirmByIdsRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTConfirmedReportService
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTMasterService
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
class AdminPPTMasterController(
    private val adminPPTMasterService: AdminPPTMasterService,
    private val pptConfirmedReportService: AdminPPTConfirmedReportService,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    /**
     * 전문행사조 화면 지점 셀렉터 옵션 — 마스터/이력/확정인원 3화면 공용.
     *
     * 여사원 일정/대시보드와 동일하게 [WomenScheduleBranchResolver] 로 권한별 지점 화이트리스트를 산출한다
     * (단일 출처). 목록은 곧 해당 사용자가 조회 허용된 지점이며, 각 목록 조회 endpoint 는 DataScope 로
     * 동일 화이트리스트를 재적용해 임의 branchCode 조회(IDOR) 를 차단한다.
     */
    @GetMapping("/api/v1/admin/ppt-masters/branches")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = womenScheduleBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean
    ): ResponseEntity<ApiResponse<PPTMasterListResponse>> {
        val response = adminPPTMasterService.getMasters(
            scope, employeeName, employeeCode, teamType, branchCode, validOnly,
            PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 전문행사조 확정 인원 보고서 조회 (Spec #846). isConfirmed=true.
     *
     * 지점 스코프 — 마스터/이력 조회와 동일하게 본인 소속 지점만 노출하며, 전사 권한은 전체를 본다.
     * `branchCode` 지정 시(다중지점 사용자가 지점 선택) 해당 지점만 필터.
     */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ApiResponse<PPTConfirmedReportResponse>> {
        val response = pptConfirmedReportService.getReport(scope, branchCode)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 전문행사조 확정 인원 엑셀 다운로드. */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ByteArray> {
        val result = pptConfirmedReportService.exportReport(scope, branchCode)
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.getMaster(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun createMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterCreateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.createMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun updateMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PPTMasterUpdateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.updateMaster(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun deleteMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        adminPPTMasterService.deleteMaster(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/admin/ppt-masters/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean
    ): ResponseEntity<ByteArray> {
        val result = adminPPTMasterService.exportToExcel(
            scope, employeeName, employeeCode, teamType, branchCode, validOnly
        )
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-masters/excel-template")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun downloadExcelTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ByteArray> {
        val bytes = adminPPTMasterService.generateExcelTemplate()
        val filename = "전문행사조마스터_템플릿_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ExcelResponseUtils.build(bytes, filename)
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun validateBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkValidationResponse>> {
        val response = adminPPTMasterService.validateBulk(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk/confirm")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun confirmBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkConfirmResponse>> {
        val response = adminPPTMasterService.confirmBulk(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/confirm-by-ids")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun confirmByIds(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterConfirmByIdsRequest
    ): ResponseEntity<ApiResponse<ConfirmByIdsResponse>> {
        val response = adminPPTMasterService.confirmByIds(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-masters/{masterId}/history")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable masterId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getHistory(masterId, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-histories/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportHistories(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtTo: LocalDate?
    ): ResponseEntity<ByteArray> {
        val result = adminPPTMasterService.exportHistoryToExcel(
            scope, employeeName, employeeCode, teamType, branchCode, changedAtFrom, changedAtTo
        )
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-histories")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getAllHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtTo: LocalDate?
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getAllHistory(
            scope, employeeName, employeeCode, teamType, branchCode, changedAtFrom, changedAtTo,
            PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
