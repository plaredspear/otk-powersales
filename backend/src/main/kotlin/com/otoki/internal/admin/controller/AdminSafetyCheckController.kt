package com.otoki.internal.admin.controller

import com.otoki.internal.safetycheck.dto.response.SafetyCheckStatusResponse
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.safetycheck.service.AdminSafetyCheckService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.admin.exception.InvalidDateFormatException
import com.otoki.internal.common.security.UserPrincipal
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
    @RequiresPermission(AdminPermission.SAFETY_CHECK_READ)
    fun getStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
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

        val result = adminSafetyCheckService.getStatus(principal.userId, targetDate)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
