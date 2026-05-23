package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.schedule.dto.request.AdminAttendanceLogSearchRequest
import com.otoki.powersales.schedule.dto.response.AdminAttendanceLogDetailResponse
import com.otoki.powersales.schedule.dto.response.AdminAttendanceLogListItemResponse
import com.otoki.powersales.schedule.service.AdminAttendanceLogService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/attendance-log")
class AdminAttendanceLogController(
    private val service: AdminAttendanceLogService,
) {

    @GetMapping
    @RequiresSfPermission(entity = "attendance_log", operation = SfPermissionOperation.READ)
    fun list(
        @ModelAttribute filter: AdminAttendanceLogSearchRequest,
        @RequestParam(name = "page", defaultValue = "0") page: Int,
        @RequestParam(name = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<AdminAttendanceLogListItemResponse>>> {
        val pageable = AdminAttendanceLogService.normalizePageable(
            PageRequest.of(page.coerceAtLeast(0), size.coerceAtLeast(1))
        )
        return ResponseEntity.ok(ApiResponse.success(service.search(filter, pageable)))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "attendance_log", operation = SfPermissionOperation.READ)
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminAttendanceLogDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(service.get(id)))
    }
}
