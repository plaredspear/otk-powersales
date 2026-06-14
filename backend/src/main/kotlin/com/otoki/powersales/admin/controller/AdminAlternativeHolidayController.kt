package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.org.leave.dto.request.AlternativeHolidayApproveRequest
import com.otoki.powersales.domain.org.leave.dto.request.AlternativeHolidayCreateRequest
import com.otoki.powersales.domain.org.leave.dto.request.AlternativeHolidayRejectRequest
import com.otoki.powersales.domain.org.leave.dto.response.AlternativeHolidayApproveResponse
import com.otoki.powersales.domain.org.leave.dto.response.AlternativeHolidayCreateResponse
import com.otoki.powersales.domain.org.leave.dto.response.AlternativeHolidayListItem
import com.otoki.powersales.domain.org.leave.dto.response.AlternativeHolidayRejectResponse
import com.otoki.powersales.domain.org.leave.service.AdminAlternativeHolidayService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/alternative-holidays")
class AdminAlternativeHolidayController(
    private val adminAlternativeHolidayService: AdminAlternativeHolidayService
) {

    @GetMapping
    @RequiresSfPermission(entity = "alternative_holiday", operation = SfPermissionOperation.READ)
    fun getAlternativeHolidays(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) orgCode: String?
    ): ResponseEntity<ApiResponse<List<AlternativeHolidayListItem>>> {
        val response = adminAlternativeHolidayService.getAlternativeHolidays(
            startDate, endDate, status, employeeCode, orgCode
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresSfPermission(entity = "alternative_holiday", operation = SfPermissionOperation.CREATE)
    fun createAlternativeHoliday(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AlternativeHolidayCreateRequest
    ): ResponseEntity<ApiResponse<AlternativeHolidayCreateResponse>> {
        val response = adminAlternativeHolidayService.createAlternativeHoliday(
            request, principal.requireEmployeeId()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping("/{id}/approve")
    @RequiresSfPermission(entity = "alternative_holiday", operation = SfPermissionOperation.EDIT)
    fun approveAlternativeHoliday(
        @PathVariable id: Long,
        @RequestBody request: AlternativeHolidayApproveRequest
    ): ResponseEntity<ApiResponse<AlternativeHolidayApproveResponse>> {
        val response = adminAlternativeHolidayService.approveAlternativeHoliday(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{id}/reject")
    @RequiresSfPermission(entity = "alternative_holiday", operation = SfPermissionOperation.EDIT)
    fun rejectAlternativeHoliday(
        @PathVariable id: Long,
        @Valid @RequestBody request: AlternativeHolidayRejectRequest
    ): ResponseEntity<ApiResponse<AlternativeHolidayRejectResponse>> {
        val response = adminAlternativeHolidayService.rejectAlternativeHoliday(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
