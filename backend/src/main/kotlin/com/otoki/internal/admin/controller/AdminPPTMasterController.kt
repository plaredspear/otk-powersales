package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.PPTMasterBulkValidateRequest
import com.otoki.internal.admin.dto.request.PPTMasterCreateRequest
import com.otoki.internal.admin.dto.request.PPTMasterUpdateRequest
import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.admin.service.AdminPPTMasterService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
class AdminPPTMasterController(
    private val adminPPTMasterService: AdminPPTMasterService
) {

    @GetMapping("/api/v1/admin/ppt-masters")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getMasters(
        @AuthenticationPrincipal principal: UserPrincipal,
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

    @GetMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getMaster(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.getMaster(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun createMaster(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PPTMasterCreateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.createMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun updateMaster(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PPTMasterUpdateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.updateMaster(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun deleteMaster(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        adminPPTMasterService.deleteMaster(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/admin/ppt-masters/excel-template")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun downloadExcelTemplate(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ByteArray> {
        val bytes = adminPPTMasterService.generateExcelTemplate()
        val filename = "전문행사조마스터_템플릿_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes)
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun validateBulk(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkValidationResponse>> {
        val response = adminPPTMasterService.validateBulk(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk/confirm")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun confirmBulk(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkConfirmResponse>> {
        val response = adminPPTMasterService.confirmBulk(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-masters/{masterId}/history")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getHistory(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable masterId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getHistory(masterId, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
