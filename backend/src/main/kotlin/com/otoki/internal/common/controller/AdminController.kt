package com.otoki.internal.common.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val authService: AuthService
) {

    @PostMapping("/users/{employeeCode}/reset-device")
    @PreAuthorize("hasRole('ADMIN')")
    fun resetDevice(@PathVariable employeeCode: String): ResponseEntity<ApiResponse<Any?>> {
        authService.resetDevice(employeeCode)
        return ResponseEntity.ok(ApiResponse.success(null, "단말기 등록이 초기화되었습니다"))
    }
}
