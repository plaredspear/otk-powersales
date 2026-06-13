package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckStatusResponse
import com.otoki.powersales.safetycheck.service.AdminSafetyCheckService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.admin.exception.InvalidDateFormatException
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/v1/admin/safety-check")
class AdminSafetyCheckController(
    private val adminSafetyCheckService: AdminSafetyCheckService
) {

    @GetMapping("/status")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun getStatus(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) date: String?
    ): ResponseEntity<ApiResponse<SafetyCheckStatusResponse>> {
        val targetDate = if (date != null) {
            try {
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                throw InvalidDateFormatException()
            }
        } else {
            LocalDate.now()
        }

        val result = adminSafetyCheckService.getStatus(principal.requireEmployeeId(), targetDate)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
