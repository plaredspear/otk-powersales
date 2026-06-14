package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.ConfirmByIdsResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterResponse
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterConfirmByIdsRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTConfirmedReportService
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTMasterService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
) {

    @GetMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean
    ): ResponseEntity<ApiResponse<PPTMasterListResponse>> {
        val response = adminPPTMasterService.getMasters(
            employeeName, employeeCode, teamType, branchCode, validOnly,
            PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 전문행사조 확정 인원 보고서 조회 (Spec #846). isConfirmed=true 전사. */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<PPTConfirmedReportResponse>> {
        val response = pptConfirmedReportService.getReport()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 전문행사조 확정 인원 엑셀 다운로드. */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report/export")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun exportConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ByteArray> {
        val result = pptConfirmedReportService.exportReport()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${result.filename}\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(result.bytes)
    }

    @GetMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.getMaster(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun createMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterCreateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.createMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun updateMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PPTMasterUpdateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.updateMaster(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun deleteMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        adminPPTMasterService.deleteMaster(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/admin/ppt-masters/export")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun exportMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean
    ): ResponseEntity<ByteArray> {
        val bytes = adminPPTMasterService.exportToExcel(
            employeeName, employeeCode, teamType, branchCode, validOnly
        )
        val filename = "전문행사조마스터_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes)
    }

    @GetMapping("/api/v1/admin/ppt-masters/excel-template")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun downloadExcelTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ByteArray> {
        val bytes = adminPPTMasterService.generateExcelTemplate()
        val filename = "전문행사조마스터_템플릿_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes)
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun validateBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkValidationResponse>> {
        val response = adminPPTMasterService.validateBulk(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk/confirm")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun confirmBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkConfirmResponse>> {
        val response = adminPPTMasterService.confirmBulk(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/confirm-by-ids")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun confirmByIds(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterConfirmByIdsRequest
    ): ResponseEntity<ApiResponse<ConfirmByIdsResponse>> {
        val response = adminPPTMasterService.confirmByIds(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-masters/{masterId}/history")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable masterId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getHistory(masterId, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-histories")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getAllHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtTo: LocalDate?
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getAllHistory(
            employeeName, employeeCode, teamType, changedAtFrom, changedAtTo,
            PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
